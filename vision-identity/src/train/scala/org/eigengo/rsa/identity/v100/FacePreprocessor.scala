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

import java.io.File

import scala.util.Random

object FacePreprocessor {

  def main(args: Array[String]): Unit = {
    val brightnessPreprocessors = (-10 to 10).map(new Preprocessors.Brightness(_))
    val blurPreprocessors = (1 to 4).filterNot(_ % 2 == 0).map(x ⇒ new Preprocessors.Blur(x * 3))
    val rotatePreprocessors = List.fill(10)(new Preprocessors.Rotate((Random.nextDouble() - 0.5) * 30))

    val preprocessors =
      List(new Preprocessors.ExtractFaces("/haarcascade_frontalface_default.xml")) ++
      rotatePreprocessors ++
      brightnessPreprocessors ++
      blurPreprocessors

    val result = new PreprocessingPipeline(
      new File("/Users/janmachacek/Eigengo/reactive-summit-2016-data/faces-raw"),
      new File("/Users/janmachacek/Eigengo/reactive-summit-2016-data/faces"),
      preprocessors
    ).preprocess()

    println(result)
  }

}
