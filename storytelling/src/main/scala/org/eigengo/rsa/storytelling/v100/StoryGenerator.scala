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

import scala.io.Source

class StoryGenerator private(network: MultiLayerNetwork, characters: List[Char]) {

  def generate(initialization: String): String = {
    network.rnnClearPreviousState()
    network.rnnTimeStep()
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
