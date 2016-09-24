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
import java.util.Random

import org.datavec.api.io.filters.BalancedPathFilter
import org.datavec.api.io.labels.ParentPathLabelGenerator
import org.datavec.api.split.{FileSplit, InputSplit}
import org.datavec.image.loader.BaseImageLoader
import org.datavec.image.recordreader.ImageRecordReader
import org.datavec.image.transform.{FlipImageTransform, ImageTransform, WarpImageTransform}
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator
import org.deeplearning4j.datasets.iterator.MultipleEpochsIterator
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.layers._
import org.deeplearning4j.nn.conf.{GradientNormalization, MultiLayerConfiguration, NeuralNetConfiguration, Updater}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.util.NetSaverLoaderUtils
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.lossfunctions.LossFunctions

import scala.util.Try

object FaceTrainer {
  val height = 50
  val width = 50
  val channels = 3
  val numExamples = 200
  val numLabels = 5
  val batchSize = 20
  val listenerFreq = 1
  val iterations = 1
  val epochs = 5
  val splitTrainTest = 0.8
  val nCores = 8
  // num of cores on machine to paralize data load
  val rng = new Random()

  def main(args: Array[String]) {
    val mainPath = new File("/Users/janmachacek/Eigengo/reactive-summit-2016-data/faces")

    // Define how to filter and load data into batches
    val fileSplit: FileSplit = new FileSplit(mainPath, BaseImageLoader.ALLOWED_FORMATS, rng)
    val pathFilter: BalancedPathFilter = new BalancedPathFilter(rng, BaseImageLoader.ALLOWED_FORMATS, new ParentPathLabelGenerator(), numExamples, numLabels, 0, batchSize)

    // Define train and test split
    val inputSplit: Array[InputSplit] = fileSplit.sample(pathFilter, 80, 20)
    val trainData: InputSplit = inputSplit(0)
    val testData: InputSplit = inputSplit(1)

    // Define transformation
    val flipTransform: ImageTransform = new FlipImageTransform(rng)
    val warpTransform: ImageTransform = new WarpImageTransform(rng, 42)
    val transforms: List[ImageTransform] = List(null, flipTransform, warpTransform)

    // Build model based on tiny model configuration paper
    val confTiny: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
      //.seed(seed)
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
      .cnnInputSize(height, width, channels).build()

    val network: MultiLayerNetwork = new MultiLayerNetwork(confTiny)
    network.init()
    network.setListeners(new ScoreIterationListener(listenerFreq))

    // Define how to load data into network
    val recordReader: ImageRecordReader = new ImageRecordReader(height, width, channels, new ParentPathLabelGenerator())
    var dataIter: DataSetIterator = null
    var trainIter: MultipleEpochsIterator = null

    // Train
    for (transform <- transforms) Try {
      recordReader.initialize(trainData, transform)
      dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels)
      trainIter = new MultipleEpochsIterator(epochs, dataIter, nCores)
      network.fit(trainIter)
    }

    // Evaluate
    recordReader.initialize(testData)
    dataIter = new RecordReaderDataSetIterator(recordReader, 20, 1, numLabels)
    val eval = network.evaluate(dataIter)
    print(eval.stats(true))

    // Save model and parameters
    NetSaverLoaderUtils.saveNetworkAndParameters(network, "/Users/janmachacek/Tmp")
    NetSaverLoaderUtils.saveUpdators(network, "/Users/janmachacek/Tmp")

  }

}
