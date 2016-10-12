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
package org.eigengo.rsa

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaType.Compressible
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, MediaType}
import akka.http.scaladsl.unmarshalling.Unmarshaller.UnsupportedContentTypeException
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import com.google.protobuf.CodedInputStream
import com.trueaccord.scalapb.json.JsonFormat
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

import scala.annotation.StaticAnnotation
import scala.concurrent.Future

trait ScalaPBMarshalling {
  private val protobufContentType = ContentType(MediaType.applicationBinary("octet-stream", Compressible, "proto"))
  private val applicationJsonContentType = ContentTypes.`application/json`

  trait ToTextMessageMarshaller[-A] {
    def apply(value: A): TextMessage
  }

  def marshalTextMessage[A](value: A)(implicit m: ToTextMessageMarshaller[A]): TextMessage = m(value)

  implicit object StringToTextMessageMarshaller extends ToTextMessageMarshaller[String] {
    override def apply(value: String): TextMessage = TextMessage('"' + value + '"')
  }

  implicit def scalaPBToTextMessageMarshaller[A <: GeneratedMessage]: ToTextMessageMarshaller[A] = new ToTextMessageMarshaller[A] {
    override def apply(value: A): TextMessage = TextMessage(JsonFormat.toJsonString(value))
  }

  implicit def listToEntityMarshaller[A : ToTextMessageMarshaller]: ToTextMessageMarshaller[Seq[A]] = new ToTextMessageMarshaller[Seq[A]] {
    val m = implicitly[ToTextMessageMarshaller[A]]

    override def apply(value: Seq[A]): TextMessage = {
      val result = StringBuilder.newBuilder
      result.append("[")
      value.foreach { x ⇒
        if (result.length > 1) result.append(",")
        result.append(m(x) match {
          case TextMessage.Strict(text) ⇒ text
          case _ ⇒ "?"
        })
      }
      result.append("]")
      TextMessage(result.toString())
    }
  }

  @ScalaPBMarshalling.permit
  def scalaPBFromRequestUnmarshaller[O <: GeneratedMessage with Message[O]](companion: GeneratedMessageCompanion[O]): FromEntityUnmarshaller[O] = {
    Unmarshaller.withMaterializer[HttpEntity, O](_ ⇒ implicit mat ⇒ {
      case entity@HttpEntity.Strict(`applicationJsonContentType`, data) ⇒
        val charBuffer = Unmarshaller.bestUnmarshallingCharsetFor(entity)
        FastFuture.successful(JsonFormat.fromJsonString(data.decodeString(charBuffer.nioCharset().name()))(companion))
      case entity@HttpEntity.Strict(`protobufContentType`, data) ⇒
        FastFuture.successful(companion.parseFrom(CodedInputStream.newInstance(data.asByteBuffer)))
      case entity ⇒
        Future.failed(UnsupportedContentTypeException(applicationJsonContentType, protobufContentType))
    })
  }

  @ScalaPBMarshalling.permit
  implicit def scalaPBToEntityMarshaller[U <: GeneratedMessage]: ToEntityMarshaller[U] = {
    def jsonMarshaller(): ToEntityMarshaller[U] = {
      val contentType = applicationJsonContentType
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

object ScalaPBMarshalling extends ScalaPBMarshalling {
  private class permit extends StaticAnnotation
}
