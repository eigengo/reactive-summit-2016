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
package org.eigengo.rsa.storytelling.v100

import cats.data.Xor
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.eigengo.rsa.deeplearning4j.NetworkLoader
import org.nd4j.linalg.factory.Nd4j

import scala.io.Source

class StoryGenerator private(network: MultiLayerNetwork, characters: List[Char]) {

  private def sampleFromDistribution(distribution: Seq[Double]): Int = {
    val d = math.random
    distribution.foldLeft((0, 0.0)) { case (r@(ri, sum), element) ⇒
      if (sum > d) r
      else (ri + 1, sum + element)
    }._1
  }

  def generate(inputString: String, outputLength: Int): String = {
    val input = Nd4j.zeros(1, characters.length, inputString.length)
    inputString.zipWithIndex.foreach { case (c, i) ⇒
      val ci = characters.indexOf(c)
      input.putScalar(Array(0, ci, i), 1.0)
    }

    network.rnnClearPreviousState()
    val output = network.rnnTimeStep(input)
    val lastTimeStep = output.tensorAlongDimension(output.size(2) - 1, 1, 0)
    (0 until outputLength).foldLeft(("", lastTimeStep)) { case ((text, x), _) ⇒
      val nextInput = Nd4j.zeros(1, characters.length)
      val outputProbDistribution = characters.indices.map { i ⇒ x.getDouble(0, i) }
      val sampledCharacterIdx = sampleFromDistribution(outputProbDistribution)

      nextInput.putScalar(Array[Int](0, sampledCharacterIdx), 1.0f) //Prepare next time step input

      (text + characters(sampledCharacterIdx), network.rnnTimeStep(nextInput))
    }._1
  }

}

object StoryGenerator {

  def apply(basePath: String): Throwable Xor StoryGenerator = {
    val charactersFile = s"$basePath.chars"

    for {
      network    ← NetworkLoader.loadMultiLayerNetwork(basePath)
      characters ← Xor.catchNonFatal(Source.fromFile(charactersFile).toList)
    } yield new StoryGenerator(network, characters)

  }

}
