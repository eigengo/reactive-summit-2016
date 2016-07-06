package org.eigengo.rsa.captioning

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshalling, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentType, ContentTypes, MessageEntity}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.ActorMaterializer
import org.eigengo.protobufcheck.{ProtobufGen, ProtobufMatchers}
import org.eigengo.rsa.ScalaPBMarshalling
import org.eigengo.rsa.captioning.v200.Caption.Item.Kind
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class PropertyTest extends FlatSpec with ProtobufMatchers with PropertyChecks with ScalaPBMarshalling {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def inOut[A](value: A, contentTypes: ContentType*)(implicit marshaller: ToEntityMarshaller[A], unmarshaller: FromEntityUnmarshaller[A]): Seq[(MessageEntity, A)] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val futureResults = contentTypes.map { contentType ⇒
      marshaller.apply(value)
        .flatMap {
          _.flatMap {
            case Marshalling.WithFixedContentType(`contentType`, m) ⇒ Some(m())
            case _ ⇒ None
          } match {
            case h :: Nil ⇒ Future.successful(h)
            case _ ⇒ Future.failed(new RuntimeException(":("))
          }
        }
        .flatMap(entity ⇒ unmarshaller.apply(entity).map(value ⇒ (entity, value)))
    }

    val results = Await.result(Future.sequence(futureResults), Duration.Inf)
    results.foreach { case (e, v) ⇒ v should equal(value) }

    results
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

    implicit val _ = scalaPBFromRequestUnmarshaller(v200.Caption)

    inOut(x, ContentTypes.`application/json`, ContentTypes.`application/octet-stream`)

    println(inOut(x, ContentTypes.`application/json`))
    println(inOut(x, ContentTypes.`application/octet-stream`))
  }

}
