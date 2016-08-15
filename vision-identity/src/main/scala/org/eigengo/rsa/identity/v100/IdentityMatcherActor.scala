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

import java.io.ByteArrayInputStream

import akka.actor.{OneForOneStrategy, Props, SupervisorStrategy}
import akka.persistence.PersistentActor
import akka.routing.RandomPool
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe, Unsubscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import cakesolutions.kafka.{KafkaConsumer, KafkaDeserializer, KafkaProducer, KafkaProducerRecord}
import com.google.protobuf.ByteString
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.StringDeserializer
import org.eigengo.rsa.Envelope
import org.eigengo.rsa.deeplearning4j.NetworkLoader

import scala.util.Success

object IdentityMatcherActor {

  import FaceExtractor._

  private case class IdentifyFace(correlationId: String, handle: String, faceRGBBitmap: Array[Byte])

  private val extractor = ConsumerRecords.extractor[String, Envelope]

  def props(config: Config): Props = {

    def isFaceAcceptable(faceImage: FaceImage): Boolean = {
      val aspectRatio = faceImage.w.toDouble / faceImage.h.toDouble

      faceImage.confidence > 0.5 &&
        faceImage.w > 50 && faceImage.h > 50 &&
        aspectRatio > 0.5 && aspectRatio < 1.5
    }

    val faceExtractor = new FaceExtractor(isFaceAcceptable)

    val Success(identityMatcher) = IdentityMatcher(
      NetworkLoader.fallbackResourceAccessor(
        NetworkLoader.filesystemResourceAccessor("/opt/models/scene"),
        NetworkLoader.filesystemResourceAccessor("/Users/janmachacek/Dropbox/Models/scene")
      )
    )
    val consumerConf = KafkaConsumer.Conf(
      config.getConfig("kafka.consumer-config"),
      keyDeserializer = new StringDeserializer,
      valueDeserializer = KafkaDeserializer(Envelope.parseFrom)
    )
    val consumerActorConf = KafkaConsumerActor.Conf()

    Props(classOf[IdentityMatcherActor], consumerConf, consumerActorConf, faceExtractor, identityMatcher).withRouter(RandomPool(nrOfInstances = 10))
  }

}

class IdentityMatcherActor(consumerConf: KafkaConsumer.Conf[String, Envelope], consumerActorConf: KafkaConsumerActor.Conf,
                           producerConf: KafkaProducer.Conf[String, Envelope],
                           faceExtractor: FaceExtractor, identityMatcher: IdentityMatcher) extends PersistentActor {

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

  @scala.throws(classOf[Exception])
  override def preStart(): Unit = {
    kafkaConsumerActor ! Subscribe.AutoPartition(Seq("tweet-image"))
  }

  @scala.throws(classOf[Exception])
  override def postStop(): Unit = {
    kafkaConsumerActor ! Unsubscribe
  }

  override def receiveRecover: Receive = {
    case IdentifyFace(correlationId, handle, faceRGBBitmap) ⇒
      identityMatcher.identify(new ByteArrayInputStream(faceRGBBitmap)).foreach { identity ⇒
        val out = Envelope(version = 100,
          timestamp = System.nanoTime(),
          correlationId = correlationId,
          headers = Map(),
          payload = ByteString.copyFrom(identity.toByteArray))
        producer.send(KafkaProducerRecord(handle, out))
      }
  }

  override def receiveCommand: Receive = {
    case extractor(consumerRecords) ⇒
      consumerRecords.pairs.foreach {
        case (None, _) ⇒
        case (Some(handle), envelope) ⇒
          val is = new ByteArrayInputStream(envelope.payload.toByteArray)
          faceExtractor.extract(is).foreach { result ⇒
            persistAll(result)(fi ⇒ self ! IdentifyFace(envelope.correlationId, handle, fi.rgbBitmap))
          }
          kafkaConsumerActor ! Confirm(consumerRecords.offsets, commit = true)
      }
    case IdentifyFace(correlationId, handle, faceRGBBitmap) ⇒
      identityMatcher.identify(new ByteArrayInputStream(faceRGBBitmap)).foreach { identity ⇒
        val response = Envelope(version = 100,
          timestamp = System.nanoTime(),
          correlationId = correlationId,
          headers = Map(),
          payload = ByteString.copyFrom(identity.toByteArray))
        producer.send(KafkaProducerRecord(handle, response))
      }
  }

}
