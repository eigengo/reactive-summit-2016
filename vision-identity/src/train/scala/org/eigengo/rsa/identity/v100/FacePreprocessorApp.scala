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

import org.bytedeco.javacpp.opencv_core.{Mat, RectVector}
import org.bytedeco.javacpp.opencv_imgproc.{COLOR_BGRA2GRAY, cvtColor, equalizeHist}
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier
import org.eigengo.rsa.deeplearning4j.train.{ImagePreprocessingPipelineApp, ImagePreprocessor}

import scala.util.Random

object FacePreprocessorApp extends ImagePreprocessingPipelineApp {
  val brightnessPreprocessors = (-10 to 10).map(new ImagePreprocessor.Brightness(_))
  val blurPreprocessors = (1 to 4).filterNot(_ % 2 == 0).map(x ⇒ new ImagePreprocessor.Blur(x * 3))
  val rotatePreprocessors = List.fill(10)(new ImagePreprocessor.Rotate((Random.nextDouble() - 0.5) * 30))

  override val preprocessors: List[ImagePreprocessor] =
    List(new ExtractFaces("/haarcascade_frontalface_default.xml")) ++
      rotatePreprocessors ++
      brightnessPreprocessors ++
      blurPreprocessors

  override def sourceDirectory: File = new File("/Users/janmachacek/Eigengo/reactive-summit-2016-data/faces-raw")
  override def targetDirectory: File = new File("/Users/janmachacek/Eigengo/reactive-summit-2016-data/faces")

  class ExtractFaces(cascadeResource: String) extends ImagePreprocessor {
    private val cascadeClassifier = {
      val file = FaceExtractor.getClass.getResource(cascadeResource).getFile
      new CascadeClassifier(file)
    }

    override def preprocess(mat: Mat): List[Mat] = {
      val faces = new RectVector()
      val grayMat = new Mat()
      cvtColor(mat, grayMat, COLOR_BGRA2GRAY)
      equalizeHist(grayMat, grayMat)
      cascadeClassifier.detectMultiScale(grayMat, faces)

      (0 until faces.size().toInt).map { x ⇒
        val rect = faces.get(x)
        val submat = new Mat(mat, rect)
        submat
      }.toList
    }
  }

}
