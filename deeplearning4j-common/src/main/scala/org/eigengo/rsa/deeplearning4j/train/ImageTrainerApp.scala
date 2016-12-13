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
package org.eigengo.rsa.deeplearning4j.train

import java.io.{File, FileOutputStream, FilenameFilter}
import java.util.Random

import org.datavec.api.io.filters.BalancedPathFilter
import org.datavec.api.io.labels.ParentPathLabelGenerator
import org.datavec.api.split.{FileSplit, InputSplit}
import org.datavec.image.loader.BaseImageLoader
import org.datavec.image.recordreader.ImageRecordReader
import org.datavec.image.transform.{FlipImageTransform, ImageTransform, WarpImageTransform}
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator
import org.deeplearning4j.datasets.iterator.MultipleEpochsIterator
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.util.NetSaverLoaderUtils

import scala.util.Try

trait ImageTrainerApp {
  def height = 50
  def width = 50
  def channels = 3

  def numExamples: Int = {
    def count(f: File): Int = {
      f.listFiles().foldLeft(0) { (res, file) ⇒
        if (file.getName.startsWith(".")) res
        else if (file.isDirectory) res + count(file)
        else res + 1
      }
    }

    count(sourceDirectory)
  }

  def numLabels: Int = {
    sourceDirectory.listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = !name.startsWith(".")
    }).length
  }

  def batchSize = 20
  def listenerFreq = 1
  def iterations = 10
  def epochs = 7
  def splitTrainTest = 0.8

  def sourceDirectory: File
  def targetDirectory: File

  def networkConfiguration: MultiLayerConfiguration

  private val nCores = 8
  private val rng = new Random()

  System.setProperty("OMP_NUM_THREADS", nCores.toString)

  // Define how to filter and load data into batches
  val fileSplit: FileSplit = new FileSplit(sourceDirectory, BaseImageLoader.ALLOWED_FORMATS, rng)
  val pathFilter: BalancedPathFilter = new BalancedPathFilter(rng, BaseImageLoader.ALLOWED_FORMATS, new ParentPathLabelGenerator(), numExamples, numLabels, 0, batchSize)

  // Define train and test split
  val inputSplit: Array[InputSplit] = fileSplit.sample(pathFilter, 80, 20)
  val trainData: InputSplit = inputSplit(0)
  val testData: InputSplit = inputSplit(1)

  // Define transformation
  val flipTransform: ImageTransform = new FlipImageTransform(rng)
  val warpTransform: ImageTransform = new WarpImageTransform(rng, 42)
  val transforms: List[ImageTransform] = List(null, flipTransform, warpTransform)

  val network = new MultiLayerNetwork(networkConfiguration)
  network.init()
  network.setListeners(new ScoreIterationListener(listenerFreq))

  // Define how to load data into network
  val recordReader: ImageRecordReader = new ImageRecordReader(height, width, channels, new ParentPathLabelGenerator())

  // Train
  for (transform <- transforms) Try {
    recordReader.initialize(trainData, transform)
    val dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels)
    val trainIter = new MultipleEpochsIterator(epochs, dataIter, nCores)
    network.fit(trainIter)
  }

  // Evaluate
  recordReader.initialize(testData)
  private val dataIter = new RecordReaderDataSetIterator(recordReader, 20, 1, numLabels)
  private val eval = network.evaluate(dataIter)
  print(eval.stats(true))

  // Save model and parameters
  import scala.collection.JavaConversions._

  new FileOutputStream(new File(targetDirectory, "labels")).write(recordReader.getLabels.mkString("\n").getBytes)
  NetSaverLoaderUtils.saveNetworkAndParameters(network, targetDirectory.getAbsolutePath)
  NetSaverLoaderUtils.saveUpdators(network, targetDirectory.getAbsolutePath)

}
