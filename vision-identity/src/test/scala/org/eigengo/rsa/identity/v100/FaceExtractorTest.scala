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
package org.eigengo.rsa.identity.v100

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.util.Success

class FaceExtractorTest extends FlatSpec with PropertyChecks with Matchers with Inside {
  lazy val Success(faceExtractor) = FaceExtractor()

  def getResourceBytes(resourceName: String): Array[Byte] = {
    val is = getClass.getResourceAsStream(resourceName)
    Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
  }

  it should "indicate failures" in {
    faceExtractor.extract(Array(1: Byte)).isFailure shouldBe true
    faceExtractor.extract(null).isFailure shouldBe true
  }

  it should "not find Helena in salad" in {
    faceExtractor.extract(getResourceBytes("/salad.jpg")).get shouldBe empty
  }

  it should "find faces in image" in {
    faceExtractor.extract(getResourceBytes("/dogface.jpg")).get shouldBe empty

    inside(faceExtractor.extract(getResourceBytes("/verified.jpg")).get) {
      case FaceImage(_, x, y, w, h, _)::Nil ⇒
        x should be > 60
        y should be > 30
        w should be > 100
        h should be > 100
      case _ ⇒ fail()
    }

    inside(faceExtractor.extract(getResourceBytes("/impostor.jpg")).get) {
      case FaceImage(_, x, y, w, h, _)::Nil ⇒
        x should be > 65
        y should be > 45
        w should be > 160
        h should be > 160
      case _ ⇒ fail()
    }
  }

}
