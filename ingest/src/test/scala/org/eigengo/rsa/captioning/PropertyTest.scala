package org.eigengo.rsa.captioning

import org.eigengo.protobufcheck.X
import org.scalatest.FlatSpec

class PropertyTest extends FlatSpec {

  "Generated code should" should "foo" in {
    val c100 = v100.Caption("a", 1)

    val c101 = X.inout(c100, v101.Caption)

    println(c101)
  }

}
