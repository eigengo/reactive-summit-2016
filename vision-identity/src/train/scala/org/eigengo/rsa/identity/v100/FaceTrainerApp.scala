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

import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers._
import org.deeplearning4j.nn.conf.{GradientNormalization, MultiLayerConfiguration, NeuralNetConfiguration, Updater}
import org.deeplearning4j.nn.weights.WeightInit
import org.eigengo.rsa.deeplearning4j.train.ImageTrainerApp
import org.nd4j.linalg.lossfunctions.LossFunctions

object FaceTrainerApp extends ImageTrainerApp {
  override def sourceDirectory: File = new File("/Users/janmachacek/Eigengo/reactive-summit-2016-data/")
  override def targetDirectory: File = new File("/Users/janmachacek/Tmp")

  override def networkConfiguration: MultiLayerConfiguration =
    new NeuralNetConfiguration.Builder()
      .seed(seed)
      .iterations(iterations)
      .activation("relu")
      .weightInit(WeightInit.XAVIER)
      .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
      .updater(Updater.NESTEROVS)
      .learningRate(0.01)
      .momentum(0.9)
      .regularization(true)
      .l2(0.04)
      .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
      .useDropConnect(true)
      .list()
      .layer(0, new ConvolutionLayer.Builder(5, 5)
        .name("cnn1")
        .nIn(channels)
        .stride(1, 1)
        .padding(2, 2)
        .nOut(32)
        .build())
      .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
        .kernelSize(3, 3)
        .name("pool1")
        .build())
      .layer(2, new LocalResponseNormalization.Builder(3, 5e-05, 0.75).build())
      .layer(3, new ConvolutionLayer.Builder(5, 5)
        .name("cnn2")
        .stride(1, 1)
        .padding(2, 2)
        .nOut(32)
        .build())
      .layer(4, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
        .kernelSize(3, 3)
        .name("pool2")
        .build())
      .layer(5, new LocalResponseNormalization.Builder(3, 5e-05, 0.75).build())
      .layer(6, new ConvolutionLayer.Builder(5, 5)
        .name("cnn3")
        .stride(1, 1)
        .padding(2, 2)
        .nOut(64)
        .build())
      .layer(7, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
        .kernelSize(3, 3)
        .name("pool3")
        .build())
      .layer(8, new DenseLayer.Builder()
        .name("ffn1")
        .nOut(250)
        .dropOut(0.5)
        .build())
      .layer(9, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
        .nOut(numLabels)
        .activation("softmax")
        .build())
      .backprop(true).pretrain(false)
      .setInputType(InputType.convolutional(height, width, channels))
      .build()

}
