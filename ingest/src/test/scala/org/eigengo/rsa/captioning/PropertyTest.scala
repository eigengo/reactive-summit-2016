package org.eigengo.rsa.captioning

import org.eigengo.protobufcheck.ProtobufMatchers
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class PropertyTest extends FlatSpec with ProtobufMatchers with GeneratorDrivenPropertyChecks {

  "Generated code should" should "foo" in {
    v100.Caption("a", 1) should be (compatibleWith(v101.Caption))
    v101.Caption("a", 1, Some(true)) should be (compatibleWith(v100.Caption))
  }

}
