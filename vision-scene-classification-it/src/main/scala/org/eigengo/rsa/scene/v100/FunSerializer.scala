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

import java.util

import org.apache.kafka.common.serialization.Serializer

/**
  * Wraps ``fun`` into ``Serializer[A]``, which can be used in the Kafka producer
  *
  * @param fun the (pure) function to be turned in to a Serializer
  */
class FunSerializer[A](fun: A ⇒ Array[Byte]) extends Serializer[A] {

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = { }

  override def serialize(topic: String, data: A): Array[Byte] = fun(data)

  override def close(): Unit = { }
}
