package org.eigengo.rsa

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaType.Compressible
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, MediaType}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import com.trueaccord.scalapb.json.JsonFormat
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

import scala.concurrent.Future

trait ScalaPBMarshalling {
  private val protobufContentType = ContentType(MediaType.applicationBinary("octet-stream", Compressible, "proto"))

  def scalaPBFromRequestUnmarshaller[O <: GeneratedMessage with Message[O]](companion: GeneratedMessageCompanion[O]): FromEntityUnmarshaller[O] = {
    Unmarshaller.withMaterializer[HttpEntity, O](_ ⇒ implicit mat ⇒ {
      case entity@HttpEntity.Strict(ContentTypes.`application/json`, data) ⇒
        val charBuffer = Unmarshaller.bestUnmarshallingCharsetFor(entity)
        FastFuture.successful(JsonFormat.fromJsonString(data.decodeString(charBuffer.nioCharset().name()))(companion))
      case entity@HttpEntity.Strict(`protobufContentType`, data) ⇒
        FastFuture.successful(companion.parseFrom(data.asByteBuffer.array()))
      case entity ⇒
        // TODO: Fix me
        Future.failed(new RuntimeException)
    })

    Unmarshaller.firstOf()
  }

  implicit def scalaPBToEntityMarshaller[U <: GeneratedMessage]: ToEntityMarshaller[U] = {
    def jsonMarshaller(): ToEntityMarshaller[U] = {
      val contentType = ContentTypes.`application/json`
      Marshaller.withFixedContentType(contentType) { value ⇒
        HttpEntity(contentType, JsonFormat.toJsonString(value))
      }
    }

    def protobufMarshaller(): ToEntityMarshaller[U] = {
      Marshaller.withFixedContentType(protobufContentType) { value ⇒
        HttpEntity(protobufContentType, value.toByteArray)
      }
    }

    Marshaller.oneOf(jsonMarshaller(), protobufMarshaller())
  }

}
