package org.eigengo.rsa.identity.v100

import java.io.InputStream

import scala.util.{Success, Try}

object FaceExtractor {

  case class FaceImage(confidence: Double, x: Int, y: Int, w: Int, h: Int, rgbBitmap: Array[Byte])

}

class FaceExtractor(acceptor: FaceExtractor.FaceImage ⇒ Boolean) {
  import FaceExtractor._

  def extract(imageStream: InputStream): Try[List[FaceImage]] = Success(List(
    FaceImage(1.0, 0, 0, 100, 100, Array.empty),
    FaceImage(1.0, 0, 0, 100, 100, Array.empty),
    FaceImage(1.0, 0, 0, 100, 100, Array.empty)
  ))

}
