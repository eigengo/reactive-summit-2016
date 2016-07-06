package org.eigengo.rsa.captioning

import akka.http.scaladsl.marshalling.{Marshalling, ToEntityMarshaller}
import org.eigengo.protobufcheck.{ProtobufGen, ProtobufMatchers}
import org.eigengo.rsa.ScalaPBMarshalling
import org.eigengo.rsa.captioning.v200.Caption.Item.Kind
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PropertyTest extends FlatSpec with ProtobufMatchers with PropertyChecks with ScalaPBMarshalling {

  "Same major versions" should "be compatible with each other" in {
    v100.Caption("a", 1) should be (compatibleWith(v101.Caption))
    v101.Caption("a", 1, Some(true)) should be (compatibleWith(v100.Caption))

    forAll(ProtobufGen.message(v101.Caption))(_ should be (compatibleWith(v100.Caption)))
  }

  "Marshalling" should "work" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    val x = v200.Caption(
      text = "Hello,",
      ints = Seq(1, 2, 3),
      items = Seq(v200.Caption.Item(accuracy = 1, kind = Kind.Category("foo")), v200.Caption.Item(accuracy = 1, kind = Kind.NamedPerson("bar"))),
      corpus = v200.Caption.Corpus.UNIVERSAL)
    
    val v200CaptionMarshaller = implicitly[ToEntityMarshaller[v200.Caption]]
    Await.result(v200CaptionMarshaller.apply(x).map(_.head), Duration.Inf) match {
      case Marshalling.WithFixedContentType(_, m) ⇒
        Console.println(m())
      case _ ⇒ fail("Bad match")
    }
  }

}
