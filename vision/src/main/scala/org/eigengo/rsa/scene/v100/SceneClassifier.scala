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
import org.canova.image.loader.ImageLoader
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

import scala.io.Source

/**
  * Performs classification using the loaded network and matching labels. The number
  * of elements in ``labels`` has to match the number of outputs in the ``network``.
  *
  * @param network the (trained and initialized) network
  * @param labels the human-readable names in order of network outputs
  */
class SceneClassifier private(network: MultiLayerNetwork, labels: List[String]) {
  private val loader = new ImageLoader(100, 100, 3)
  private val threshold = 0.7

  /**
    * Classifies the content of the image in the ``imageStream``.
    *
    * @param imageStream the stream containing a loadable image (i.e. png, jpeg, ...)
    * @return error or scene with labels
    */
  def classify(imageStream: InputStream): Throwable Xor Scene = {
    Xor.catchNonFatal(loader.asRowVector(imageStream)).flatMap { imageRowVector ⇒
      val predictions = network.output(imageRowVector)
      if (predictions.isRowVector) {
        val predictedLabels = (0 until predictions.columns()).flatMap { column ⇒
          val prediction = predictions.getDouble(0, column)
          if (prediction > threshold) {
            Some(Scene.Label(labels(column), prediction))
          } else None
        }
        Xor.Right(Scene(predictedLabels))
      } else Xor.left(SceneClassifier.BadPredictionsShape)
    }
  }

}

/**
  * Contains function to construct the ``SceneClassifier`` instance from a base path and
  * common error types.
  */
object SceneClassifier {

  /**
    * The network's prediction for a single row vector is not a row vector
    * (This is never expected to happen)
    */
  case object BadPredictionsShape extends Exception("Predictions are not row vector.")

  /**
    * Constructs the SceneClassifier by loading the ``MultiLayerNetwork`` from three files
    * at the given ``basePath``. The three files are
    *
    * - the network configuration in ``basePath.json``
    * - the network parameters in ``basePath.bin``
    * - the labels in ``basePath.labels``
    *
    * @param basePath the base path
    * @return error or constructed classifier
    */
  def apply(basePath: String): Throwable Xor SceneClassifier = {

    def loadNetworkConfiguration(configFile: String): Throwable Xor MultiLayerConfiguration = Xor.catchNonFatal {
      if (!new File(configFile).exists()) Xor.left(new FileNotFoundException(configFile))
      val configJson = Source.fromFile(configFile).mkString
      MultiLayerConfiguration.fromJson(configJson)
    }

    def loadParams(paramsFile: String): Throwable Xor INDArray = Xor.catchNonFatal {
      if (!new File(paramsFile).exists()) Xor.left(new FileNotFoundException(paramsFile))
      val is = new DataInputStream(new BufferedInputStream(new FileInputStream(paramsFile)))
      val params = Nd4j.read(is)
      is.close()
      params
    }

    def initializeNetwork(configuration: MultiLayerConfiguration, params: INDArray): MultiLayerNetwork = {
      val network = new MultiLayerNetwork(configuration)
      network.init()
      network.setParams(params)
      network
    }

    val configFile = s"$basePath.json"
    val paramsFile = s"$basePath.bin"
    val labelsFile = s"$basePath.labels"

    for {
      configuration ← loadNetworkConfiguration(configFile)
      params ← loadParams(paramsFile)
      labels ← Xor.catchNonFatal(Source.fromFile(labelsFile).getLines().toList)
      network = initializeNetwork(configuration, params)
    } yield new SceneClassifier(network, labels)
  }

}
