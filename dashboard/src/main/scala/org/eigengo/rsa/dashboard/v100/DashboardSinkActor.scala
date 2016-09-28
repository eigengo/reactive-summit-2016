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
package org.eigengo.rsa.dashboard.v100

import akka.actor.{Actor, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.RandomPool
import cakesolutions.kafka._
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe, Unsubscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.StringDeserializer
import org.eigengo.rsa._

object DashboardSinkActor {

  private val extractor = ConsumerRecords.extractor[String, Envelope]

  def props(config: Config): Props = {
    val consumerConf = KafkaConsumer.Conf(
      config.getConfig("kafka.consumer-config"),
      keyDeserializer = new StringDeserializer,
      valueDeserializer = KafkaDeserializer(Envelope.parseFrom)
    )
    val consumerActorConf = KafkaConsumerActor.Conf()

    Props(classOf[DashboardSinkActor], consumerConf, consumerActorConf)
  }

}

class DashboardSinkActor(consumerConf: KafkaConsumer.Conf[String, Envelope], consumerActorConf: KafkaConsumerActor.Conf) extends Actor {
  import DashboardSinkActor._

  private[this] val kafkaConsumerActor = context.actorOf(
    KafkaConsumerActor.props(consumerConf = consumerConf, actorConf = consumerActorConf, downstreamActor = self)
  )

  import scala.concurrent.duration._
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10.seconds) {
    case _ ⇒ SupervisorStrategy.Restart
  }

  override def preStart(): Unit = {
    kafkaConsumerActor ! Subscribe.AutoPartition(Seq("identity", "scene"))
  }

  override def postStop(): Unit = {
    kafkaConsumerActor ! Unsubscribe
  }

  override def receive: Receive = {
    case extractor(consumerRecords) ⇒
      consumerRecords.pairs.foreach {
        case (None, _) ⇒
          context.system.log.warning("Received (None, _) from Kafka.")
        case (Some(handle), envelope) ⇒
          context.system.eventStream.publish(
            TweetEnvelope(version = 100,
              handle = handle,
              ingestionTimestamp = envelope.ingestionTimestamp,
              messageId = envelope.messageId,
              messageType = envelope.messageType,
              payload = envelope.payload))
      }
      kafkaConsumerActor ! Confirm(consumerRecords.offsets, commit = true)
  }

}
