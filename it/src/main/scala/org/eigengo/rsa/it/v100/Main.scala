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

import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord, KafkaSerializer}
import com.google.protobuf.ByteString
import com.typesafe.config.{ConfigFactory, ConfigResolveOptions}
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.eigengo.rsa.Envelope
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}

object Main {
  private val logger = LoggerFactory.getLogger(Main.getClass)

  def main(args: Array[String]): Unit = {
    val count = 100

    Option(System.getenv("START_DELAY")).foreach(d ⇒ Thread.sleep(d.toInt))

    import scala.concurrent.ExecutionContext.Implicits.global
    val config = ConfigFactory.load("it.conf").resolve(ConfigResolveOptions.defaults())

    val producer = KafkaProducer(KafkaProducer.Conf(
      config.getConfig("tweet-image-producer"),
      new StringSerializer,
      KafkaSerializer[Envelope](_.toByteArray)
    ))

    val is = getClass.getResourceAsStream("/beer.jpg")
    val bytes = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray

    while (true) {
      val futures: Seq[Future[RecordMetadata]] = (0 until count).map { _ ⇒
        val payload = ByteString.copyFrom(bytes)
        val ret = producer.send(KafkaProducerRecord("tweet-image", "@honzam399", Envelope(payload = payload)))
        print(".")
        ret
      }
      val future = Future.sequence(futures)

      import scala.concurrent.duration._
      logger.info(Await.result(future, 1.minute).toString())

      Thread.sleep(10000)
    }
    producer.close()
  }

}
