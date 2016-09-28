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

import akka.actor.{Actor, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.RandomPool
import cakesolutions.kafka._
import cakesolutions.kafka.akka.KafkaConsumerActor.{Subscribe, Unsubscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.eigengo.rsa.Envelope
import org.eigengo.rsa.deeplearning4j.NetworkLoader

import scala.util.Success

object IdentityMatcherActorSupervisor {

  def props(config: Config): Props = {
    val Success(faceExtractor) = FaceExtractor()

    val Success(identityMatcher) = IdentityMatcher(
      NetworkLoader.fallbackResourceAccessor(
        NetworkLoader.filesystemResourceAccessor("/opt/models/identity"),
        NetworkLoader.filesystemResourceAccessor("/Users/janmachacek/Dropbox/Models/identity")
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

    Props(classOf[IdentityMatcherActorSupervisor], consumerConf, consumerActorConf, producerConf, faceExtractor, identityMatcher)
  }

}

class IdentityMatcherActorSupervisor(consumerConf: KafkaConsumer.Conf[String, Envelope], consumerActorConf: KafkaConsumerActor.Conf,
                                     producerConf: KafkaProducer.Conf[String, Envelope],
                                     faceExtractor: FaceExtractor, identityMatcher: IdentityMatcher) extends Actor {

  private[this] val identityMatcherActor = context.actorOf(
    IdentityMatcherActor.props(producerConf, faceExtractor, identityMatcher).withRouter(RandomPool(nrOfInstances = 10))
  )
  private[this] val kafkaConsumerActor = context.actorOf(
    KafkaConsumerActor.props(consumerConf = consumerConf, actorConf = consumerActorConf, downstreamActor = identityMatcherActor)
  )

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ ⇒ SupervisorStrategy.Escalate
  }

  override def preStart(): Unit = {
    kafkaConsumerActor ! Subscribe.AutoPartition(Seq("tweet-image"))
  }

  override def postStop(): Unit = {
    kafkaConsumerActor ! Unsubscribe
  }

  override def receive: Receive = Actor.ignoringBehavior
}
