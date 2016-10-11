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
package org.eigengo.rsa.text.v100

import java.util

import akka.util.ByteString
import com.lightbend.lagom.javadsl.api.deser.MessageSerializer
import com.lightbend.lagom.javadsl.api.deser.MessageSerializer.{NegotiatedDeserializer, NegotiatedSerializer}
import com.lightbend.lagom.javadsl.api.transport.MessageProtocol
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

class ScalaPBMessageSerializer[M <: GeneratedMessage](companion: GeneratedMessageCompanion[_ <: M with Message[_]]) extends MessageSerializer[M, ByteString] {
  import org.eigengo.rsa.text.v100.ScalaPBMessageSerializer._

  override def deserializer(protocol: MessageProtocol): NegotiatedDeserializer[M, ByteString] =
    new ScalaPBNegotiatedDeserializer[M](companion)

  override def serializerForResponse(acceptedMessageProtocols: util.List[MessageProtocol]): NegotiatedSerializer[M, ByteString] =
    new ScalaPBNegotiatedSerializer[M]

  override def serializerForRequest(): NegotiatedSerializer[M, ByteString] =
    new ScalaPBNegotiatedSerializer[M]

}

object ScalaPBMessageSerializer {
  class ScalaPBNegotiatedSerializer[M <: GeneratedMessage] extends NegotiatedSerializer[M, ByteString] {
    override def serialize(messageEntity: M): ByteString = ByteString(messageEntity.toByteArray)
  }

  class ScalaPBNegotiatedDeserializer[M <: GeneratedMessage](companion: GeneratedMessageCompanion[_ <: M with Message[_]]) extends NegotiatedDeserializer[M, ByteString] {
    override def deserialize(wire: ByteString): M = companion.parseFrom(wire.toArray)
  }
}