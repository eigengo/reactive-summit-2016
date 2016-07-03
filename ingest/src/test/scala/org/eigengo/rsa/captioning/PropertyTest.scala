package org.eigengo.rsa.captioning

import org.eigengo.protobufcheck.{ProtobufGen, ProtobufMatchers}
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks

class PropertyTest extends FlatSpec with ProtobufMatchers with PropertyChecks {

  "Same major versions" should "be compatible with each other" in {
    v100.Caption("a", 1) should be (compatibleWith(v101.Caption))
    v101.Caption("a", 1, Some(true)) should be (compatibleWith(v100.Caption))

    forAll(ProtobufGen.message(v200.Caption))(println)

    forAll(ProtobufGen.message(v101.Caption))(_ should be (compatibleWith(v100.Caption)))
  }

}
