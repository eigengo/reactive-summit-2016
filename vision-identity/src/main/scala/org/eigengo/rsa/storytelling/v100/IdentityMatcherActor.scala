package org.eigengo.rsa.storytelling.v100

import java.io.ByteArrayInputStream

import akka.actor.{Actor, Props}
import akka.routing.RandomPool
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import cakesolutions.kafka.{KafkaConsumer, KafkaDeserializer}
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.StringDeserializer
import org.eigengo.rsa.Envelope
import org.eigengo.rsa.deeplearning4j.NetworkLoader

import scala.util.Success

object IdentityMatcherActor {
  private val extractor = ConsumerRecords.extractor[String, Envelope]

  def props(config: Config): Props = {
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

    Props(classOf[IdentityMatcherActor], consumerConf, consumerActorConf, identityMatcher).withRouter(RandomPool(nrOfInstances = 10))
  }

}

class IdentityMatcherActor(consumerConf: KafkaConsumer.Conf[String, Envelope], consumerActorConf: KafkaConsumerActor.Conf, identityMatcher: IdentityMatcher) extends Actor {
  import IdentityMatcherActor._

  private[this] val kafkaConsumerActor = {
    context.actorOf(
      KafkaConsumerActor.props(consumerConf = consumerConf, actorConf = consumerActorConf, downstreamActor = self),
      "KafkaConsumer"
    )
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    kafkaConsumerActor ! Subscribe.AutoPartition(Seq("tweet-image"))
  }

  override def receive: Receive = {
    case extractor(consumerRecords) ⇒
      consumerRecords.pairs.foreach {
        case (None, _) ⇒
          context.system.log.info("###### Bantha poodoo!")
        case (Some(handle), envelope) ⇒
          context.system.log.info(s"###### Received ${envelope.payload.size()}")
          identityMatcher.identify(new ByteArrayInputStream(envelope.payload.toByteArray)).foreach(x ⇒ context.system.log.info(s"###### $x"))
          context.system.log.info(s"###### End")
          kafkaConsumerActor ! Confirm(consumerRecords.offsets, commit = true)
      }
  }

}
