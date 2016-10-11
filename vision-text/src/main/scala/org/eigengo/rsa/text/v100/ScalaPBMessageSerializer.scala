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