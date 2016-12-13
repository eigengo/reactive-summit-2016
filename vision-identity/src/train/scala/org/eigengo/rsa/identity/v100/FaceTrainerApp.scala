package org.eigengo.rsa.identity.v100

import java.io.File

import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.eigengo.rsa.deeplearning4j.train.ImageTrainerApp

object FaceTrainerApp extends ImageTrainerApp {
  override def sourceDirectory: File = new File("/Users/janmachacek/Eigengo/reactive-summit-2016-data/")
  override def targetDirectory: File = new File("/Users/janmachacek/Tmp")
  override def networkConfiguration: MultiLayerConfiguration = new AlexNet(height, width, channels, numLabels, 100, iterations).conf()
}
