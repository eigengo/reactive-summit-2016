package org.eigengo.rsa

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaType.Compressible
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, MediaType}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import com.trueaccord.scalapb.json.JsonFormat
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

trait ScalaPBMarshalling {

  def scalaPBFromRequestUnmarshaller[O <: GeneratedMessage with Message[O]](companion: GeneratedMessageCompanion[O]): FromEntityUnmarshaller[O] = {


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
      val contentType = ContentType(MediaType.applicationBinary("octet-stream", Compressible, "proto"))
      Marshaller.withFixedContentType(contentType) { value ⇒
        HttpEntity(contentType, value.toByteArray)
      }
    }

    Marshaller.oneOf(jsonMarshaller(), protobufMarshaller())
  }

}
