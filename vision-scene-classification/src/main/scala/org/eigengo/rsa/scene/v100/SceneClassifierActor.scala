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

import akka.actor.{Actor, Props}
import cakesolutions.kafka.KafkaConsumer
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.StringDeserializer
import org.eigengo.rsa.Envelope
import org.eigengo.rsa.deeplearning4j.NetworkLoader

import scala.util.Success

object SceneClassifierActor {
  private val extractor = ConsumerRecords.extractor[String, Envelope]

  def props(config: Config): Props = {
    val Success(sceneClassifier) = SceneClassifier(NetworkLoader.classpathResourceAccessor(getClass, "/models/scene/"))

    val consumerConf = KafkaConsumer.Conf(
      config.getConfig("kafka.consumer-config"),
      keyDeserializer = new StringDeserializer,
      valueDeserializer = new FunDeserializer(Envelope.parseFrom)
    )
    val consumerActorConf = KafkaConsumerActor.Conf(config.getConfig("kafka.consumer-actor-config"))
    Props(classOf[SceneClassifierActor], consumerConf, consumerActorConf, sceneClassifier)
  }

}

class SceneClassifierActor(consumerConf: KafkaConsumer.Conf[_, _], consumerActorConf: KafkaConsumerActor.Conf, sceneClassifier: SceneClassifier) extends Actor {
  import SceneClassifierActor._
  private[this] val kafkaConsumerActor = context.actorOf(
    KafkaConsumerActor.props(consumerConf = consumerConf, actorConf = consumerActorConf, downstreamActor = self),
    "KafkaConsumer"
  )

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    kafkaConsumerActor ! Subscribe()
  }

  override def receive: Receive = {
    case extractor(consumerRecords) ⇒
      consumerRecords.pairs.foreach {
        case (None, _) ⇒
          println("Bantha poodoo!")
        case (Some(handle), envelope) ⇒
          println(s"Received ${envelope.payload.size()}")
          sceneClassifier.classify(new ByteArrayInputStream(envelope.payload.toByteArray)).foreach(println)
          kafkaConsumerActor ! Confirm(consumerRecords.offsets, commit = true)
      }
  }

}
