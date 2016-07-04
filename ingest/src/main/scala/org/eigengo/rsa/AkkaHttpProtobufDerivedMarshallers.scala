package org.eigengo.rsa

import akka.http.scaladsl.marshalling.{Marshaller, PredefinedToEntityMarshallers, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, MessageEntity}
import com.google.protobuf.GeneratedMessage

trait AkkaHttpProtobufDerivedMarshallers extends PredefinedToEntityMarshallers {

  def toHttpEntity(entity: GeneratedMessage): MessageEntity = {
    HttpEntity.Empty
  }

  implicit def protobufDerivedMarshaller[T <: GeneratedMessage](): ToEntityMarshaller[T] =
    Marshaller.withFixedContentType(ContentTypes.`application/json`)(toHttpEntity)

}
