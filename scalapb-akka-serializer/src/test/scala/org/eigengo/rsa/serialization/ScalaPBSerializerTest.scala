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
package org.eigengo.rsa.serialization

import akka.actor.{ActorSystem, ExtendedActorSystem}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks

class ScalaPBSerializerTest extends FlatSpec with PropertyChecks with Matchers {
  val system = ActorSystem().asInstanceOf[ExtendedActorSystem]
  val serializer = new ScalaPBSerializer(system)

  it should "handle ScalaPB instance" in {
    val in = SomeMessage(version = 1, text = "foo")

    val manifest = serializer.manifest(in)
    val binary = serializer.toBinary(in)
    val out = serializer.fromBinary(binary, manifest)

    in shouldBe out
  }

  it should "reject non-ScalaPB instances" in {
    an[IllegalArgumentException] should be thrownBy serializer.toBinary("Hello")
  }

  it should "reject missing objects" in {
    a[ClassNotFoundException] should be thrownBy serializer.fromBinary(Array.emptyByteArray, "Foo")
  }

}
