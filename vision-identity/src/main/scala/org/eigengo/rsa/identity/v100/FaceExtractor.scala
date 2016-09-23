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

import com.google.protobuf.ByteString
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_objdetect._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_imgcodecs._

import scala.util.Try

/**
  * Performs the face extraction from a given image using a cascade classifier
  * @param faceCascade the cascade classifier
  */
class FaceExtractor private (faceCascade: CascadeClassifier) {

  /**
    * Extracts the faces in an encoding of an image `image`. Returns a Try of a
    * decoding or image manipulation error or a list of detected faces
    *
    * @param image the image
    * @return the error or detected faces, each with its coordinates in the original image and the extracted B&W image
    */
  def extract(image: Array[Byte]): Try[List[FaceImage]] = Try {
    val mat = imdecode(new Mat(image, false), CV_LOAD_IMAGE_COLOR)
    if (mat.cols() == 0 || mat.rows() == 0) {
      throw new IllegalArgumentException("The image is 0 width or 0 height")
    }
    val grayMat = new Mat()
    cvtColor(mat, grayMat, COLOR_BGRA2GRAY)
    equalizeHist(grayMat, grayMat)
    val faces = new RectVector()
    faceCascade.detectMultiScale(grayMat, faces)

    (0 until faces.size().toInt).map { x ⇒
      val rect = faces.get(x)
      val submat = new Mat(grayMat, rect)
      val bp = new BytePointer()
      imencode(".jpg", submat, bp)
      val bs = ByteString.copyFrom(bp.getStringBytes)

      FaceImage(confidence = 1.0, x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height(), rgbBitmap = bs)
    }.toList
  }

}

/**
  * Constructs the face extractor
  */
object FaceExtractor {

  /**
    * Construct the classifier and return a valid instance
    * @return the FaceExtractor instance
    */
  def apply(): Try[FaceExtractor] = Try {
    val file = FaceExtractor.getClass.getResource("/haarcascade_frontalface_default.xml").getFile
    new FaceExtractor(new CascadeClassifier(file))
  }

}
