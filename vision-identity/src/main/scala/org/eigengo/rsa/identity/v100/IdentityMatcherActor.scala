package org.eigengo.rsa.identity.v100

import java.util.UUID

import akka.actor.{Kill, OneForOneStrategy, Props, SupervisorStrategy}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import cakesolutions.kafka.akka.KafkaConsumerActor.Confirm
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import com.google.protobuf.ByteString
import org.eigengo.rsa.Envelope

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object IdentityMatcherActor {
  private val extractor = ConsumerRecords.extractor[String, Envelope]

  def props(producerConf: KafkaProducer.Conf[String, Envelope],
            identityMatcher: IdentityMatcher): Props =
    Props(classOf[IdentityMatcherActor], producerConf, identityMatcher)

}

class IdentityMatcherActor(producerConf: KafkaProducer.Conf[String, Envelope],
                           identityMatcher: IdentityMatcher)
  extends PersistentActor with AtLeastOnceDelivery {
  import IdentityMatcherActor._
  lazy val Success(faceExtractor) = FaceExtractor()

  private[this] val producer = KafkaProducer(conf = producerConf)

  import scala.concurrent.duration._

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10.seconds) {
    case _ ⇒ SupervisorStrategy.Restart
  }

  override val persistenceId: String = "identity-matcher-actor"

  def identifyFacesAndSend(identifyFaces: Seq[IdentifyFace])(implicit executor: ExecutionContext): Future[Unit] = {
    val sentFutures = identifyFaces.flatMap { identifyFace ⇒
      faceExtractor.extract(identifyFace.image.toByteArray).map(_.map { faceImage ⇒
        val face = identityMatcher.identify(faceImage.rgbBitmap.newInput()) match {
          case Some(identifiedFace) ⇒ Identity.Face.IdentifiedFace(identifiedFace)
          case None ⇒ Identity.Face.UnknownFace(Identity.UnknownFace())
        }
        val identity = Identity(face = face)
        val out = Envelope(version = 100,
          processingTimestamp = System.nanoTime(),
          ingestionTimestamp = identifyFace.ingestionTimestamp,
          correlationId = identifyFace.correlationId,
          messageId = UUID.randomUUID().toString,
          messageType = "identity",
          payload = ByteString.copyFrom(identity.toByteArray))
        producer.send(KafkaProducerRecord("identity", identifyFace.handle, out)).map(_ ⇒ Unit)
      }).getOrElse(Nil)
    }

    Future.sequence(sentFutures).map(_ ⇒ Unit)
  }

  def handleIdentifyFace: Receive = {
    case (deliveryId: Long, identifyFaces: IdentifyFaces) ⇒
      import context.dispatcher
      identifyFacesAndSend(identifyFaces.identifyFaces).onSuccess { case _ ⇒ confirmDelivery(deliveryId) }
    case IdentifyFaces(faces) ⇒
      import context.dispatcher
      identifyFacesAndSend(faces).onFailure { case _ ⇒ self ! Kill }
  }

  override def receiveRecover: Receive = handleIdentifyFace

  override def receiveCommand: Receive = handleIdentifyFace orElse {
    case extractor(consumerRecords) ⇒
      val identifyFaces = consumerRecords.pairs.map {
        case (_, envelope) ⇒ IdentifyFace(envelope.ingestionTimestamp, envelope.correlationId, envelope.handle, envelope.payload)
      }

      persist(IdentifyFaces(identifyFaces = identifyFaces)) { result ⇒
        deliver(self.path)(deliveryId ⇒ (deliveryId, result))
        sender() ! Confirm(consumerRecords.offsets, commit = true)
      }
    case KafkaConsumerActor.BackingOff(_) ⇒
      context.system.log.warning("Backing off!")
  }

}
