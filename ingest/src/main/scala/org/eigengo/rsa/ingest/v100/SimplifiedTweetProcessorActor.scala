package org.eigengo.rsa.ingest.v100

import java.util.UUID

import akka.actor.{Actor, OneForOneStrategy, Props, SupervisorStrategy}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.stream.ActorMaterializer
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord, KafkaSerializer}
import com.google.protobuf.ByteString
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.StringSerializer
import org.eigengo.rsa.Envelope

object SimplifiedTweetProcessorActor {

  def props(config: Config): Props = {
    val producerConf = KafkaProducer.Conf(
      config.getConfig("tweet-image-producer"),
      new StringSerializer,
      KafkaSerializer[Envelope](_.toByteArray)
    )
    Props(classOf[SimplifiedTweetProcessorActor], producerConf)
  }
}

class SimplifiedTweetProcessorActor(producerConf: KafkaProducer.Conf[String, Envelope]) extends Actor {
  private[this] val producer = KafkaProducer(conf = producerConf)
  implicit val _ = ActorMaterializer()

  import scala.concurrent.duration._
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10.seconds) {
    case _ ⇒ SupervisorStrategy.Restart
  }

  override def receive: Receive = {
    case TweetImage(handle, content) ⇒
      producer.send(KafkaProducerRecord("tweet-image", handle,
        Envelope(version = 100,
          ingestionTimestamp = System.nanoTime(),
          processingTimestamp = System.nanoTime(),
          messageId = UUID.randomUUID().toString,
          correlationId = UUID.randomUUID().toString,
          payload = content)))
    case SimplifiedTweet(handle, mediaUrls) ⇒
      mediaUrls.foreach { mediaUrl ⇒
        import context.dispatcher
        val request = HttpRequest(method = HttpMethods.GET, uri = Uri(mediaUrl))
        val timeout = 1000.millis
        Http(context.system).singleRequest(request).flatMap(_.entity.toStrict(timeout)).foreach { entity ⇒
          self ! TweetImage(handle, ByteString.copyFrom(entity.data.toArray))
        }
      }
  }

}
