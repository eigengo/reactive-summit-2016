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
package org.eigengo.rsa.identity.v100;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.distribution.GaussianDistribution;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Cifar10 Caffe Model Variations
 * <p>
 * Quick Model based on:
 * http://caffe.berkeleyvision.org/gathered/examples/cifar10.html
 * https://github.com/BVLC/caffe/blob/master/examples/cifar10/cifar10_quick_train_test.prototxt
 * <p>
 * Full Sigmoid Model based on:
 * https://github.com/BVLC/caffe/blob/master/examples/cifar10/cifar10_full_solver_lr1.prototxt
 * https://github.com/BVLC/caffe/blob/master/examples/cifar10/cifar10_full_train_test.prototxt
 * <p>
 * BatchNorm Model based on:
 * https://github.com/BVLC/caffe/blob/master/examples/cifar10/cifar10_full_sigmoid_solver_bn.prototxt
 * https://github.com/BVLC/caffe/blob/master/examples/cifar10/cifar10_full_sigmoid_train_test_bn.prototxt
 */
public class CifarCaffeModels {

    enum CifarModeEnum {
        CAFFE_BATCH_NORM, CAFFE_FULL_SIGMOID, CAFFE_QUICK, TORCH_NIN, TORCH_VGG, OTHER;
    }

    private int height;
    private int width;
    private int channels;
    private int numLabels;
    private long seed;
    private int iterations;
    private int[] nIn;
    private int[] nOut;
    private String activation;
    private WeightInit weightInit;
    private OptimizationAlgorithm optimizationAlgorithm;
    private Updater updater;
    private LossFunctions.LossFunction lossFunctions;
    private double learningRate;
    private double biasLearningRate;
    private boolean regularization;
    private double l2;
    private double momentum;

    private MultiLayerConfiguration conf;

    public CifarCaffeModels(int height, int width, int channels, int numLabels, long seed,
                            int iterations, int[] nIn, int[] nOut, String activation,
                            WeightInit weightInit, OptimizationAlgorithm optimizationAlgorithm,
                            Updater updater, LossFunctions.LossFunction lossFunctions,
                            double learningRate, double biasLearningRate,
                            boolean regularization, double l2, double momentum) {

        this.height = height;
        this.width = width;
        this.channels = channels;
        this.numLabels = numLabels;
        this.seed = seed;
        this.iterations = iterations;
        this.nIn = nIn;
        this.nOut = nOut;
        this.activation = activation;
        this.weightInit = weightInit;
        this.optimizationAlgorithm = optimizationAlgorithm;
        this.updater = updater;
        this.lossFunctions = lossFunctions;
        this.learningRate = learningRate;
        this.biasLearningRate = (biasLearningRate == Double.NaN) ? learningRate : biasLearningRate;
        this.regularization = regularization;
        this.l2 = l2;
        this.momentum = momentum;
    }


    public MultiLayerConfiguration caffeInitQuick() {
        MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .activation(activation)
                .weightInit(weightInit).dist(new GaussianDistribution(0, 1e-4))
                //.gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .learningRate(learningRate).biasLearningRate(biasLearningRate)
                .optimizationAlgo(optimizationAlgorithm)
                .learningRate(learningRate).biasLearningRate(biasLearningRate)
                .updater(updater).momentum(momentum)
                .regularization(regularization).l2(l2)
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn1")
                        .nIn(channels)
                        .nOut(nOut[0])
                        .stride(1, 1)
                        .padding(2, 2)
                        .build())
                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool1")
                        .build())
                .layer(2, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn2")
                        .nOut(nOut[1])
                        .stride(1, 1)
                        .padding(2, 2)
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .layer(3, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool2")
                        .build())
                .layer(4, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn3")
                        .nOut(nOut[2])
                        .stride(1, 1)
                        .padding(2, 2)
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .layer(5, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool3")
                        .build())
                .layer(6, new DenseLayer.Builder()
                        .name("ffn1")
                        .nOut(nOut[3])
                        .dropOut(0.5)
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .layer(7, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(numLabels)
                        .activation("softmax")
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .backprop(true).pretrain(false)
                .cnnInputSize(height, width, channels);

        return builder.build();
    }

    public MultiLayerConfiguration caffeInitFull() {
        MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .weightInit(weightInit).dist(new GaussianDistribution(0, 1e-4))
                .activation(activation)
//                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .optimizationAlgo(optimizationAlgorithm)
                .learningRate(learningRate).biasLearningRate(biasLearningRate)
                .updater(updater).momentum(momentum)
                .regularization(regularization).l2(l2)
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn1")
                        .nIn(channels)
                        .nOut(nOut[0])
                        .stride(1, 1)
                        .padding(2, 2)
                        .build())
                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool1")
                        .build())
                .layer(2, new LocalResponseNormalization.Builder(1, 5e-05, 0.75).n(3).build())
                .layer(3, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn2")
                        .nOut(nOut[1])
                        .stride(1, 1)
                        .padding(2, 2)
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .layer(4, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool2")
                        .build())
                .layer(5, new LocalResponseNormalization.Builder(1, 5e-05, 0.75).n(3).build())
                .layer(6, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn3")
                        .nOut(nOut[2])
                        .stride(1, 1)
                        .padding(2, 2)
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .layer(7, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool3")
                        .build())
                .layer(8, new DenseLayer.Builder()
                        .name("ffn1")
                        .nOut(nOut[3])
                        .dropOut(0.5)
                        .build())
                .layer(9, new OutputLayer.Builder(lossFunctions)
                        .nOut(numLabels)
                        .activation("softmax")
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .backprop(true).pretrain(false)
                .cnnInputSize(height, width, channels);
        conf = builder.build();
        return conf;

    }

    public MultiLayerConfiguration caffeInitBN() {
        MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .activation(activation)
                .weightInit(weightInit).dist(new GaussianDistribution(0, 1e-4))
                //.gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .learningRateDecayPolicy(LearningRatePolicy.Step)
                .lrPolicyDecayRate(1)
                .lrPolicySteps(5000)
                .optimizationAlgo(optimizationAlgorithm)
                .learningRate(learningRate).biasLearningRate(biasLearningRate)
                .updater(updater).momentum(momentum)
                .regularization(regularization).l2(l2)
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn1")
                        .nOut(nOut[0])
                        .activation("identity")
                        .nIn(channels)
                        .stride(1, 1)
                        .padding(2, 2)
                        .build())
                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool1")
                        .build())
                .layer(2, new BatchNormalization.Builder().build())
                .layer(3, new ActivationLayer.Builder().build())
                .layer(4, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn2")
                        .nOut(nOut[1])
                        .activation("identity")
                        .stride(1, 1)
                        .padding(2, 2)
                        .biasInit(0)
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .layer(5, new BatchNormalization.Builder().build())
                .layer(6, new ActivationLayer.Builder().build())
                .layer(7, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool2")
                        .build())
                .layer(8, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn3")
                        .nOut(nOut[2])
                        .activation("identity")
                        .stride(1, 1)
                        .padding(2, 2)
                        .biasInit(0)
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .layer(9, new BatchNormalization.Builder().build())
                .layer(10, new ActivationLayer.Builder().build())
                .layer(11, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool3")
                        .build())
                .layer(12, new OutputLayer.Builder(lossFunctions)
                        .nOut(numLabels)
                        .activation("softmax")
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .backprop(true).pretrain(false)
                .cnnInputSize(height, width, channels);

        conf = builder.build();
        return conf;
    }

    public MultiLayerConfiguration initOther() {
        MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .activation(activation)
                .weightInit(weightInit).dist(new GaussianDistribution(0, 1e-4))
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .learningRateDecayPolicy(LearningRatePolicy.Step)
                .lrPolicyDecayRate(1)
                .lrPolicySteps(5000)
                .optimizationAlgo(optimizationAlgorithm)
                .learningRate(learningRate).biasLearningRate(biasLearningRate)
                .updater(updater).momentum(momentum)
                .regularization(regularization).l2(l2)
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn1")
                        .nOut(nOut[0])
                        .activation("identity")
                        .nIn(channels)
                        .stride(1, 1)
                        .padding(2, 2)
                        .build())
                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool1")
                        .build())
                .layer(2, new BatchNormalization.Builder().build())
                .layer(3, new ActivationLayer.Builder().build())
                .layer(4, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn2")
                        .nOut(nOut[1])
                        .activation("identity")
                        .stride(1, 1)
                        .padding(2, 2)
                        .biasInit(0)
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .layer(5, new BatchNormalization.Builder().build())
                .layer(6, new ActivationLayer.Builder().build())
                .layer(7, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool2")
                        .build())
                .layer(8, new ConvolutionLayer.Builder(5, 5)
                        .name("cnn3")
                        .nOut(nOut[2])
                        .activation("identity")
                        .stride(1, 1)
                        .padding(2, 2)
                        .biasInit(0)
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .layer(9, new BatchNormalization.Builder().build())
                .layer(10, new ActivationLayer.Builder().build())
                .layer(11, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3})
                        .name("pool3")
                        .build())
                .layer(12, new OutputLayer.Builder(lossFunctions)
                        .nOut(numLabels)
                        .activation("softmax")
                        .dist(new GaussianDistribution(0, 1e-2))
                        .build())
                .backprop(true).pretrain(false)
                .cnnInputSize(height, width, channels);

        conf = builder.build();
        return conf;
    }


    public MultiLayerConfiguration torchInitNin() {
        MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .activation(activation)
                .updater(updater)
                .weightInit(weightInit).dist(new NormalDistribution(0, 0.5))
                .iterations(iterations)
                .optimizationAlgo(optimizationAlgorithm)
                .learningRate(learningRate)
                .regularization(true).l2(l2)
                .momentum(momentum)
                .list()
                .layer(0, new ConvolutionLayer.Builder(new int[]{5, 5}, new int[]{1, 1}, new int[]{2, 2})
                        .name("cnn1")
                        .nIn(channels)
                        .nOut(192)
                        .activation("identity")
                        .build())
                .layer(1, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(2, new ActivationLayer.Builder().build())
                .layer(3, new ConvolutionLayer.Builder(new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn2")
                        .nOut(160)
                        .activation("identity")
                        .build())
                .layer(4, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(5, new ActivationLayer.Builder().build())
                .layer(6, new ConvolutionLayer.Builder(new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn3")
                        .nOut(96)
                        .activation("identity")
                        .build())
                .layer(7, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(8, new ActivationLayer.Builder().build())
                .layer(9, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3}, new int[]{2, 2})
                        .name("maxpool1")
                        .build())
                .layer(10, new ConvolutionLayer.Builder(new int[]{5, 5}, new int[]{1, 1}, new int[]{2, 2})
                        .name("cnn4")
                        .nOut(192)
                        .activation("identity")
                        .dropOut(0.5)
                        .build())
                .layer(11, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(12, new ActivationLayer.Builder().build())
                .layer(13, new ConvolutionLayer.Builder(new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn5")
                        .nOut(192)
                        .activation("identity")
                        .build())
                .layer(14, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(15, new ActivationLayer.Builder().build())
                .layer(16, new ConvolutionLayer.Builder(new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn6")
                        .nOut(192)
                        .activation("identity")
                        .build())
                .layer(17, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(18, new ActivationLayer.Builder().build())
                .layer(19, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{3, 3}, new int[]{2, 2})
                        .name("maxpool2")
                        .build())
                .layer(20, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn7")
                        .nOut(192)
                        .activation("identity")
                        .dropOut(0.5)
                        .build())
                .layer(21, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(22, new ActivationLayer.Builder().build())
                .layer(23, new ConvolutionLayer.Builder(new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn8")
                        .nOut(192)
                        .activation("identity")
                        .build())
                .layer(24, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(25, new ActivationLayer.Builder().build())
                .layer(26, new ConvolutionLayer.Builder(new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn9")
                        .nOut(10)
                        .activation("identity")
                        .build())
                .layer(27, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(28, new ActivationLayer.Builder().build())
                .layer(29, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG, new int[]{6, 6}, new int[]{1, 1}) // should be 8,8
                        .name("maxpool3")
                        .build())
                .layer(30, new OutputLayer.Builder(lossFunctions)
                        .name("output")
                        .nOut(numLabels)
                        .activation("softmax")
                        .build())
                .backprop(true)
                .pretrain(false)
                .cnnInputSize(height, width, channels);

        conf = builder.build();
        return conf;


    }

    public MultiLayerConfiguration torchInitVGG() {
        MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .activation(activation)
                .updater(updater)
                .weightInit(weightInit)
                .iterations(iterations)
                .optimizationAlgo(optimizationAlgorithm)
                .learningRate(learningRate)
                .regularization(true).l2(l2)
                .momentum(momentum)
                .list()
                .layer(0, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn1")
                        .nIn(channels)
                        .nOut(64)
                        .activation("identity")
                        .dropOut(0.3)
                        .build())
                .layer(1, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(2, new ActivationLayer.Builder().build())

                .layer(3, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn2")
                        .nOut(64)
                        .activation("identity")
                        .build())
                .layer(4, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(5, new ActivationLayer.Builder().build())
                .layer(6, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{2, 2})
                        .name("maxpool1")
                        .build())
                .layer(7, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn3")
                        .nOut(128)
                        .activation("identity")
                        .dropOut(0.4)
                        .build())
                .layer(8, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(9, new ActivationLayer.Builder().build())
                .layer(10, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn4")
                        .nOut(128)
                        .activation("identity")
                        .build())
                .layer(11, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(12, new ActivationLayer.Builder().build())
                .layer(13, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{2, 2})
                        .name("maxpool2")
                        .build())
                .layer(14, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn5")
                        .nOut(256)
                        .activation("identity")
                        .dropOut(0.4)
                        .build())
                .layer(15, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(16, new ActivationLayer.Builder().build())
                .layer(17, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn6")
                        .nOut(256)
                        .activation("identity")
                        .dropOut(0.4)
                        .build())
                .layer(18, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(19, new ActivationLayer.Builder().build())
                .layer(20, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn7")
                        .nOut(256)
                        .activation("identity")
                        .build())
                .layer(21, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(22, new ActivationLayer.Builder().build())
                .layer(23, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{2, 2})
                        .name("maxpool3")
                        .build())
                .layer(24, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn8")
                        .nOut(512)
                        .activation("identity")
                        .dropOut(0.4)
                        .build())
                .layer(25, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(26, new ActivationLayer.Builder().build())
                .layer(27, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn9")
                        .nOut(512)
                        .activation("identity")
                        .dropOut(0.4)
                        .build())
                .layer(28, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(29, new ActivationLayer.Builder().build())
                .layer(30, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn10")
                        .nOut(512)
                        .activation("identity")
                        .build())
                .layer(31, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(32, new ActivationLayer.Builder().build())
                .layer(33, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{2, 2})
                        .name("maxpool4")
                        .build())
                .layer(34, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn11")
                        .nOut(512)
                        .activation("identity")
                        .dropOut(0.4)
                        .build())
                .layer(35, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(36, new ActivationLayer.Builder().build())
                .layer(37, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn12")
                        .nOut(512)
                        .activation("identity")
                        .dropOut(0.4)
                        .build())
                .layer(38, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(39, new ActivationLayer.Builder().build())
                .layer(40, new ConvolutionLayer.Builder(new int[]{3, 3}, new int[]{1, 1}, new int[]{1, 1})
                        .name("cnn13")
                        .nOut(512)
                        .activation("identity")
                        .build())
                .layer(41, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(42, new ActivationLayer.Builder().build())
                .layer(43, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX, new int[]{2, 2})
                        .name("maxpool5")
                        .build())
                .layer(44, new DenseLayer.Builder()
                        .name("ffn1")
                        .nOut(512)
                        .activation("identity")
                        .dropOut(0.5)
                        .build())
                .layer(45, new BatchNormalization.Builder().eps(1e-3).build())
                .layer(46, new ActivationLayer.Builder().build())
                .layer(47, new DenseLayer.Builder()
                        .name("ffn2")
                        .nOut(512)
                        .dropOut(0.5)
                        .build())
                .layer(48, new OutputLayer.Builder(lossFunctions)
                        .name("output")
                        .nOut(numLabels)
                        .activation("softmax")
                        .build())
                .backprop(true)
                .pretrain(false)
                .cnnInputSize(height, width, channels);

        conf = builder.build();
        return conf;

    }

    public MultiLayerNetwork buildNetwork(CifarModeEnum networkType) {
        switch (networkType) {
            case CAFFE_QUICK:
                conf = caffeInitQuick();
                break;
            case CAFFE_FULL_SIGMOID:
                conf = caffeInitFull();
                break;
            case CAFFE_BATCH_NORM:
                conf = caffeInitBN();
                break;
            case TORCH_NIN:
                conf = torchInitNin();
                break;
            case TORCH_VGG:
                conf = torchInitVGG();
                break;
            default:
                conf = initOther();
                break;
        }

        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();
        return network;
    }
}
