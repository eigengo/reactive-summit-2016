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

import java.io._

import cats.data.Xor
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.util.NetSaverLoaderUtils
import org.nd4j.linalg.factory.Nd4j

import scala.io.Source

class SceneClassifier private(network: MultiLayerNetwork) {

  def classify(scene: Array[Byte]): Throwable Xor Scene = {
    Xor.left(new NotImplementedError())
  }

}

object SceneClassifier {

  def apply(basePath: String): Throwable Xor SceneClassifier = {
    val configFile = s"$basePath.json"
    val paramsFile = s"$basePath.bin"

    if (!new File(configFile).exists()) Xor.left(new FileNotFoundException(configFile))
    else if (!new File(paramsFile).exists()) Xor.left(new FileNotFoundException(paramsFile))
    else Xor.catchNonFatal {
      val configJson = Source.fromFile(configFile).mkString
      val networkConfiguration: MultiLayerConfiguration = MultiLayerConfiguration.fromJson(configJson)
      val is = new DataInputStream(new BufferedInputStream(new FileInputStream(paramsFile)))
      val params = Nd4j.read(is)
      is.close()

      val network = new MultiLayerNetwork(networkConfiguration)
      network.init()
      network.setParams(params)
      new SceneClassifier(network)
    }
  }

}
