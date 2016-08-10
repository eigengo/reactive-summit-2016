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

import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import com.google.protobuf.ByteString
import com.typesafe.config.{ConfigFactory, ConfigResolveOptions}
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.eigengo.rsa.Envelope

import scala.concurrent.{Await, Future}

object MainTest {

  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val config = ConfigFactory.load("application.conf").resolve(ConfigResolveOptions.defaults().setAllowUnresolved(false))

    val producer = KafkaProducer(KafkaProducer.Conf(
      config.getConfig("tweet-image-producer"),
      new StringSerializer,
      new FunSerializer[Envelope](_.toByteArray)
    ))

    val futures: Seq[Future[RecordMetadata]] = (0 until 10000).map { _ ⇒
      val payload = ByteString.copyFrom(Array[Byte](1, 2, 3))
      producer.send(KafkaProducerRecord("tweet-image", "@honzam399", Envelope(payload = payload)))
    }
    val future = Future.sequence(futures)

    import scala.concurrent.duration._
    Await.result(future, 1.minute)
    producer.close()
  }

}
