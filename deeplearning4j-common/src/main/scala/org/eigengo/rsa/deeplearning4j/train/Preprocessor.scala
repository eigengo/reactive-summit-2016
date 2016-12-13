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
package org.eigengo.rsa.deeplearning4j.train

import org.bytedeco.javacpp.opencv_core._

trait Preprocessor {

  def preprocess(mat: Mat): List[Mat]

}

object Preprocessor {
  import org.bytedeco.javacpp.opencv_imgproc._

  object EqualizeHistogram extends Preprocessor {
    override def preprocess(mat: Mat): List[Mat] = {
      equalizeHist(mat, mat)
      List(mat)
    }
  }

  object Grayscale extends Preprocessor {
    override def preprocess(mat: Mat): List[Mat] = {
      cvtColor(mat, mat, COLOR_BGRA2GRAY)
      List(mat)
    }
  }

  class Brightness(delta: Double) extends Preprocessor {
    override def preprocess(mat: Mat): List[Mat] = {
      mat.convertTo(mat, -1, 1, delta)
      List(mat)
    }
  }

  class Blur(ksize: Int) extends Preprocessor {
    override def preprocess(mat: Mat): List[Mat] = {
      medianBlur(mat, mat, ksize)
      List(mat)
    }
  }

  class Rotate(degrees: Double) extends Preprocessor {
    override def preprocess(mat: Mat): List[Mat] = {
      val rm = getRotationMatrix2D(new Point2f(mat.cols() / 2, mat.rows() / 2), degrees - 180, 1)
      warpAffine(mat, mat, rm, mat.size())
      List(mat)
    }
  }

}
