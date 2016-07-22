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
package org.eigengo.rsa.scene.v100

import java.io.{File, FileInputStream, FileNotFoundException, InputStream}

import cats.data.Xor
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class SceneClassifierTest extends FlatSpec with PropertyChecks with Matchers {

  /**
    * Finds all images in classpath resource ``/scene``, and applies
    * ``block`` to each
    *
    * @param block the block to be applied to each resource under ``/scene``
    */
  private def forAllScenes[U](block: (InputStream, String) ⇒ U): Unit = {
    val scene = new File(getClass.getResource("/scene").toURI)
    scene.listFiles().foreach { file ⇒
      val splits = file.getName.split("\\.")
      val is = new FileInputStream(file)
      block(is, splits(0))
      is.close()
    }
  }

  "Missing network configuration" should "be well reported" in {
    val Xor.Left(ex) = SceneClassifier("/nothere")
    ex should be (a[FileNotFoundException])
  }

  "Image classification" should "predict correct labels" in {
    val Xor.Right(classifier) = SceneClassifier("/Users/janmachacek/Eigengo/stuff")

    forAllScenes { (stream, label) ⇒
      val Xor.Right(scene) = classifier.classify(stream)

      scene.labels.length should be (1)
      val firstLabel = scene.labels.head
      firstLabel.label should be (label)
      firstLabel.score should be > 0.8
    }
  }

}
