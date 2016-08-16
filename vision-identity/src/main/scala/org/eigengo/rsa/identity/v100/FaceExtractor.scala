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

import java.io.InputStream

import scala.util.{Success, Try}

object FaceExtractor {

  case class FaceImage(confidence: Double, x: Int, y: Int, w: Int, h: Int, rgbBitmap: Array[Byte])

}

class FaceExtractor(acceptor: FaceExtractor.FaceImage ⇒ Boolean) {
  import FaceExtractor._

  def extract(imageStream: InputStream): Try[List[FaceImage]] = Success(List(
    FaceImage(1.0, 0, 0, 100, 100, Array.empty)
  ))

}
