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
package org.eigengo.rsa.scene.v100

import java.io.ByteArrayInputStream

import akka.actor.{Actor, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.RandomPool
import cakesolutions.kafka.{KafkaConsumer, KafkaDeserializer, KafkaProducer, KafkaProducerRecord}
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe, Unsubscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import com.google.protobuf.ByteString
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.StringDeserializer
import org.eigengo.rsa.Envelope
import org.eigengo.rsa.deeplearning4j.NetworkLoader

import scala.util.Success

object SceneClassifierActor {
  private val extractor = ConsumerRecords.extractor[String, Envelope]

  def props(config: Config): Props = {
    val Success(sceneClassifier) = SceneClassifier(
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

    Props(classOf[SceneClassifierActor], consumerConf, consumerActorConf, sceneClassifier).withRouter(RandomPool(nrOfInstances = 10))
  }

}

class SceneClassifierActor(consumerConf: KafkaConsumer.Conf[String, Envelope], consumerActorConf: KafkaConsumerActor.Conf,
                           producerConf: KafkaProducer.Conf[String, Envelope],
                           sceneClassifier: SceneClassifier) extends Actor {
  import SceneClassifierActor._

  private[this] val kafkaConsumerActor = context.actorOf(
      KafkaConsumerActor.props(consumerConf = consumerConf, actorConf = consumerActorConf, downstreamActor = self),
      "KafkaConsumer"
    )
  private[this] val producer = KafkaProducer(conf = producerConf)

  import scala.concurrent.duration._
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10.seconds) {
    case _ ⇒ SupervisorStrategy.Restart
  }

  @scala.throws(classOf[Exception])
  override def preStart(): Unit = {
    kafkaConsumerActor ! Subscribe.AutoPartition(Seq("tweet-image"))
  }

  @scala.throws(classOf[Exception])
  override def postStop(): Unit = {
    kafkaConsumerActor ! Unsubscribe
  }

  override def receive: Receive = {
    case extractor(consumerRecords) ⇒
      consumerRecords.pairs.foreach {
        case (None, _) ⇒
        case (Some(handle), envelope) ⇒
          val is = new ByteArrayInputStream(envelope.payload.toByteArray)
          sceneClassifier.classify(is).foreach { scene ⇒
            val out = Envelope(version = 100,
              timestamp = System.nanoTime(),
              correlationId = envelope.correlationId,
              headers = Map(),
              payload = ByteString.copyFrom(scene.toByteArray))

            import context.dispatcher
            producer
              .send(KafkaProducerRecord(handle, out))
              .onComplete(_ ⇒ kafkaConsumerActor ! Confirm(consumerRecords.offsets, commit = true))
          }
      }
  }

}
