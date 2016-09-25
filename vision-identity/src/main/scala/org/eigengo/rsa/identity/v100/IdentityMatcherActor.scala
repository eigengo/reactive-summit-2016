/*
 * The Reactive Summit Austin talk
 * Copyright (C) 2016 Jan Machacek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.eigengo.rsa.identity.v100

import java.util.UUID

import akka.actor.{Kill, OneForOneStrategy, Props, SupervisorStrategy}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import akka.routing.RandomPool
import cakesolutions.kafka._
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe, Unsubscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import com.google.protobuf.ByteString
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.eigengo.rsa.Envelope
import org.eigengo.rsa.deeplearning4j.NetworkLoader

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object IdentityMatcherActor {

  private val extractor = ConsumerRecords.extractor[String, Envelope]

  def props(config: Config): Props = {
    val Success(faceExtractor) = FaceExtractor()

    val Success(identityMatcher) = IdentityMatcher(
      NetworkLoader.fallbackResourceAccessor(
        NetworkLoader.filesystemResourceAccessor("/opt/models/identity"),
        NetworkLoader.filesystemResourceAccessor("/Users/janmachacek/Dropbox/Models/scene")
      )
    )
    val consumerConf = KafkaConsumer.Conf(
      config.getConfig("kafka.consumer-config"),
      keyDeserializer = new StringDeserializer,
      valueDeserializer = KafkaDeserializer(Envelope.parseFrom)
    )
    val consumerActorConf = KafkaConsumerActor.Conf()
    val producerConf = KafkaProducer.Conf(
      config.getConfig("kafka.identity-producer"),
      new StringSerializer,
      KafkaSerializer[Envelope](_.toByteArray)
    )

    Props(classOf[IdentityMatcherActor], consumerConf, consumerActorConf, producerConf, faceExtractor, identityMatcher).withRouter(RandomPool(nrOfInstances = 10))
  }

}

class IdentityMatcherActor(consumerConf: KafkaConsumer.Conf[String, Envelope], consumerActorConf: KafkaConsumerActor.Conf,
                           producerConf: KafkaProducer.Conf[String, Envelope],
                           faceExtractor: FaceExtractor, identityMatcher: IdentityMatcher)
  extends PersistentActor with AtLeastOnceDelivery {

  import IdentityMatcherActor._

  private[this] val kafkaConsumerActor = context.actorOf(
      KafkaConsumerActor.props(consumerConf = consumerConf, actorConf = consumerActorConf, downstreamActor = self),
      "KafkaConsumer"
    )
  private[this] val producer = KafkaProducer(conf = producerConf)

  import scala.concurrent.duration._
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10.seconds) {
    case _ ⇒ SupervisorStrategy.Restart
  }

  override val persistenceId: String = "identity-matcher-actor"

  override def preStart(): Unit = {
    kafkaConsumerActor ! Subscribe.AutoPartition(Seq("tweet-image"))
  }

  override def postStop(): Unit = {
    kafkaConsumerActor ! Unsubscribe
  }

  def identifyFacesAndSend(identifyFaces: Seq[IdentifyFace])(implicit executor: ExecutionContext): Future[Unit] = {
    val sentFutures = identifyFaces.map { identifyFace ⇒
      val identity = identityMatcher.identify(identifyFace.rgbBitmap.newInput())
//      val out = Envelope(version = 100,
//        processingTimestamp = System.nanoTime(),
//        ingestionTimestamp = identifyFace.ingestionTimestamp,
//        correlationId = identifyFace.correlationId,
//        messageId = UUID.randomUUID().toString,
//        messageType = "identity",
//        payload = ByteString.copyFrom(identity.toByteArray))
//      producer.send(KafkaProducerRecord("identity", identifyFace.handle, out)).map(_ ⇒ Unit)
      Future.successful(Unit)
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
      val identifyFaces = consumerRecords.pairs.flatMap {
        case (None, _) ⇒ Nil
        case (Some(handle), envelope) ⇒
          val faceImages = faceExtractor.extract(envelope.payload.toByteArray).getOrElse(Nil)
          faceImages.map(x ⇒ IdentifyFace(envelope.ingestionTimestamp, envelope.correlationId, handle, x.rgbBitmap))
      }

      persist(IdentifyFaces(identifyFaces = identifyFaces)) { result ⇒
        deliver(self.path)(deliveryId ⇒ (deliveryId, result))
        kafkaConsumerActor ! Confirm(consumerRecords.offsets, commit = true)
      }
  }

}
