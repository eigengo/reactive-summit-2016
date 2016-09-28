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

import java.io.InputStream

import org.datavec.image.loader.ImageLoader
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.eigengo.rsa.deeplearning4j.NetworkLoader

import scala.io.Source
import scala.util.Try

class IdentityMatcher private(network: MultiLayerNetwork, labels: List[String]) {
  private val loader = new ImageLoader(50, 50, 3)
  private val threshold = 0.34

  def identify(imageStream: InputStream): Option[Identity.IdentifiedFace] = {
    Try(loader.asRowVector(imageStream)).toOption.flatMap { imageRowVector ⇒
      val predictions = network.output(imageRowVector)
      val (i, s) = (0 until predictions.columns()).foldLeft((0, 0.0)) {
        case (x@(bi, bs), idx) ⇒
          val s = predictions.getDouble(0, idx)
          if (s > bs) (idx, s) else x
      }
      if (s > threshold) Some(Identity.IdentifiedFace(labels(i), s)) else None
    }
  }

}

object IdentityMatcher {
  /**
    * The network's prediction for a single row vector is not a row vector
    * (This is never expected to happen)
    */
  case object BadPredictionsShape extends Exception("Predictions are not row vector.")

  def apply(resourceAccessor: NetworkLoader.ResourceAccessor): Try[IdentityMatcher] = {
    for {
      network ← NetworkLoader.loadMultiLayerNetwork(resourceAccessor)
      labels ← resourceAccessor("labels").map(is ⇒ Source.fromInputStream(is).getLines().toList)
    } yield new IdentityMatcher(network, labels)
  }

}
