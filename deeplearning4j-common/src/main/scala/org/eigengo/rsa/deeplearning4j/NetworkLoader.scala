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
package org.eigengo.rsa.deeplearning4j

import java.io.InputStream

import scala.util.{Failure, Success, Try}

/**
  * Contains convenience functions to load DL4J networks using a common naming
  * schemes.
  */
object NetworkLoader {

  type ResourceAccessor = String ⇒ Try[InputStream]
  import java.io._

  import org.deeplearning4j.nn.conf.MultiLayerConfiguration
  import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
  import org.nd4j.linalg.api.ndarray.INDArray
  import org.nd4j.linalg.factory.Nd4j

  import scala.io.Source

  /**
    * Function that can be used as the ResourceAccessor to delegate amongst many resourceAccessors,
    * where it is expected that one of them might succeed
    *
    * @param resourceAccessor the first resource accessor
    * @param resourceAccessors the remaining resource accessors
    * @param name the resource name
    * @return the resource stream
    */
  def fallbackResourceAccessor(resourceAccessor: ResourceAccessor, resourceAccessors: ResourceAccessor*)(name: String): Try[InputStream] = {
    resourceAccessors.foldLeft(resourceAccessor(name))((result, ra) ⇒ result.orElse(ra(name)))
  }

  /**
    * Function that can be used as the ResourceAccessor for filesystem resources
    * @param prefix the prefix, e.g. "/opt/models/scene"; most likely a good idea to start with "/"
    * @param name the resource name
    * @return the resource stream
    */
  def filesystemResourceAccessor(prefix: String)(name: String): Try[InputStream] = {
    Try(new FileInputStream(new File(prefix, name)))
  }

  /**
    * Function that can be used as the ResourceAccessor for classpath resources
    * @param clazz the class to load the resources from
    * @param prefix the prefix, e.g. "/models"; remember to start with "/"
    * @param name the resource name
    * @return the resource stream
    */
  def classpathResourceAccessor(clazz: Class[_], prefix: String = "/")(name: String): Try[InputStream] = {
    val resourceName = prefix + name
    val is = getClass.getResourceAsStream(resourceName)
    if (is == null) {
      Failure(new FileNotFoundException(resourceName))
    } else {
      Success(is)
    }
  }

  /**
    * Constructs the ``MultiLayerNetwork`` from two files
    * at the given ``basePath``. The three files are
    *
    * - the network configuration in resource named ``config``
    * - the network parameters in resource named ``params``
    *
    * @param resourceAccessor the accessor for the given resource
    * @return error or loaded & initialized ``MultiLayerNetwork``
    */
  def loadMultiLayerNetwork(resourceAccessor: ResourceAccessor): Try[MultiLayerNetwork] = {

    def loadNetworkConfiguration(): Try[MultiLayerConfiguration] =
      resourceAccessor("config").flatMap { config ⇒ Try {
        val configJson = Source.fromInputStream(config).mkString
        MultiLayerConfiguration.fromJson(configJson)
      }
      }

    def loadParams(): Try[INDArray] =
      resourceAccessor("params").flatMap { params ⇒ Try {
        val is = new DataInputStream(new BufferedInputStream(params))
        val result = Nd4j.read(is)
        is.close()
        result
      }
      }

    def initializeNetwork(configuration: MultiLayerConfiguration, params: INDArray): MultiLayerNetwork = {
      val network = new MultiLayerNetwork(configuration)
      network.init()
      network.setParams(params)
      network
    }

    for {
      configuration ← loadNetworkConfiguration()
      params ← loadParams()
    } yield initializeNetwork(configuration, params)

  }

}
