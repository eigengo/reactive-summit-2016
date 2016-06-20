package org.eigengo.rsa.captioning

import org.eigengo.protobufcheck.ProtobufAssertions
import org.scalatest.FlatSpec

class PropertyTest extends FlatSpec with ProtobufAssertions {

  "Generated code should" should "foo" in {
    assertForwardCompatible(v100.Caption("a", 1), v101.Caption)
  }

}
