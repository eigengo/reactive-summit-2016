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

class FaceExtractorTest extends FlatSpec with PropertyChecks with Matchers with Inside {
  def getResourceBytes(resourceName: String): Array[Byte] = {
    val is = getClass.getResourceAsStream(resourceName)
    Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
  }

  it should "find faces in image" in {
    FaceExtractor().extract(getResourceBytes("/dogface.jpg")) shouldBe empty

    inside(FaceExtractor().extract(getResourceBytes("/verified.jpg"))) {
      case FaceImage(_, x, y, w, h, _)::Nil ⇒
        x should be > 60
        y should be > 30
        w should be > 100
        h should be > 100
      case _ ⇒ fail()
    }

    inside(FaceExtractor().extract(getResourceBytes("/impostor.jpg"))) {
      case FaceImage(_, x, y, w, h, _)::Nil ⇒
        x should be > 65
        y should be > 45
        w should be > 180
        h should be > 180
      case _ ⇒ fail()
    }
  }

}
