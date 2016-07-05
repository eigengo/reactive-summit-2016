package org.eigengo.rsa.captioning

import akka.http.scaladsl.marshalling.{Marshalling, ToEntityMarshaller}
import org.eigengo.protobufcheck.{ProtobufGen, ProtobufMatchers}
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks

class PropertyTest extends FlatSpec with ProtobufMatchers with PropertyChecks {

  "Same major versions" should "be compatible with each other" in {
    v100.Caption("a", 1) should be (compatibleWith(v101.Caption))
    v101.Caption("a", 1, Some(true)) should be (compatibleWith(v100.Caption))

    forAll(ProtobufGen.message(v101.Caption))(_ should be (compatibleWith(v100.Caption)))
  }

//  "Marshalling" should "work" in {
//    import scala.concurrent.ExecutionContext.Implicits.global
//
//    val x = v200.Caption()
//    implicit val v200CaptionMarshaller: ToEntityMarshaller[v200.Caption] = protobufDerivedMarshaller()
//    v200CaptionMarshaller.apply(x).map(_.head).foreach {
//      case Marshalling.WithFixedContentType(_, m) ⇒ println(m())
//      case _ ⇒ fail("Bad marshaller")
//    }
//  }

}
