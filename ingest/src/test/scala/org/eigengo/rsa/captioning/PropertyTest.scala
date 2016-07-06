package org.eigengo.rsa.captioning

import akka.http.scaladsl.marshalling.{Marshalling, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentType, ContentTypes, MessageEntity}
import org.eigengo.protobufcheck.{ProtobufGen, ProtobufMatchers}
import org.eigengo.rsa.ScalaPBMarshalling
import org.eigengo.rsa.captioning.v200.Caption.Item.Kind
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class PropertyTest extends FlatSpec with ProtobufMatchers with PropertyChecks with ScalaPBMarshalling {

  def awaitMarshal[A](value: A, contentType: ContentType)(implicit marshaller: ToEntityMarshaller[A]): MessageEntity = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val fme = marshaller.apply(value).flatMap { marshallers ⇒
      marshallers.flatMap {
        case Marshalling.WithFixedContentType(`contentType`, m) ⇒ Some(m())
        case _ ⇒ None
      } match {
        case h::Nil ⇒ Future.successful(h)
        case _ ⇒ Future.failed(new RuntimeException(":("))
      }
    }
    Await.result(fme, Duration.Inf)
  }

  "Same major versions" should "be compatible with each other" in {
    v100.Caption("a", 1) should be (compatibleWith(v101.Caption))
    v101.Caption("a", 1, Some(true)) should be (compatibleWith(v100.Caption))

    forAll(ProtobufGen.message(v101.Caption))(_ should be (compatibleWith(v100.Caption)))
  }

  "Marshalling" should "work" in {
    val x = v200.Caption(
      text = "Hello,",
      ints = Seq(1, 2, 3),
      items = Seq(v200.Caption.Item(accuracy = 1, kind = Kind.Category("foo")), v200.Caption.Item(accuracy = 1, kind = Kind.NamedPerson("bar"))),
      corpus = v200.Caption.Corpus.UNIVERSAL)

    println(awaitMarshal(x, ContentTypes.`application/json`))
    println(awaitMarshal(x, ContentTypes.`application/octet-stream`))
  }

}
