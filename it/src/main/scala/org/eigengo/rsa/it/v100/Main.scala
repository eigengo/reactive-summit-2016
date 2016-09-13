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
package org.eigengo.rsa.it.v100

import java.util.UUID

import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord, KafkaSerializer}
import com.google.protobuf.ByteString
import com.typesafe.config.{ConfigFactory, ConfigResolveOptions}
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.eigengo.rsa.Envelope
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}
import scala.util.{Random, Try}

object Main {
  private val logger = LoggerFactory.getLogger(Main.getClass)

  def main(args: Array[String]): Unit = {
    val handles = List("@honzam399", "@anirvan_c", "@alexlashford", "@odersky", "@jamieallen", "@jonasboner")

    Option(System.getenv("START_DELAY")).foreach(d ⇒ Thread.sleep(d.toInt))

    import scala.concurrent.ExecutionContext.Implicits.global
    val config = ConfigFactory.load("it.conf").resolve(ConfigResolveOptions.defaults())

    val producer = KafkaProducer(KafkaProducer.Conf(
      config.getConfig("tweet-image-producer"),
      new StringSerializer,
      KafkaSerializer[Envelope](_.toByteArray)
    ))

    val is = getClass.getResourceAsStream("/salad.jpg")
    val bytes = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray

    while (true) {
      val futures: Seq[Future[RecordMetadata]] = Random.shuffle(handles).take(Random.nextInt(2)).flatMap { handle ⇒
        val payload = ByteString.copyFrom(bytes)
        val ret = Try(producer.send(KafkaProducerRecord("tweet-image", handle,
          Envelope(version = 100,
            ingestionTimestamp = System.nanoTime(),
            processingTimestamp = System.nanoTime(),
            messageId = UUID.randomUUID().toString,
            correlationId = UUID.randomUUID().toString,
            payload = payload))))
        ret.toOption
      }
      val future = Future.sequence(futures)

      import scala.concurrent.duration._
      logger.info(Await.result(future, 1.minute).toString())

      Thread.sleep(500)
    }
    producer.close()
  }

}
