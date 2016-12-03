package com.xbi.contents.mining;

import com.xbi.contents.mining.tools.CourseVectorSerializer;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.UiServer;
import org.deeplearning4j.ui.weights.HistogramIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.List;

/**
 * Created by Guangwen Liu on 2016/07/02.
 */
public class CourseClassifier {
    public static void main(String[] args) throws Exception {
        int seed = 123;
        double learningRate = 0.005;
        int batchSize = 64;
        int nEpochs = 100;

        int numInputs = 100;
        int numOutputs = 9;
        int numHiddenNodes = 200;

        /*
        //Load the training data:
        RecordReader rr = new CSVRecordReader();
        rr.initialize(new FileSplit(new File("src/main/resources/doc/doc.train")));
        DataSetIterator trainIter = new RecordReaderDataSetIterator(rr, batchSize, -1, numOutputs);

        //Load the test/evaluation data:
        RecordReader rrTest = new CSVRecordReader();
        rrTest.initialize(new FileSplit(new File("src/main/resources/doc/doc.test")));
        DataSetIterator testIter = new RecordReaderDataSetIterator(rrTest, batchSize, -1, numOutputs);
        */

        DataSetIterator allData = CourseVectorSerializer.loadCourseVectors();
        numOutputs = CourseVectorSerializer.getLabelIds().size();

       // allData.next().shuffle();
        SplitTestAndTrain testAndTrain = allData.next().splitTestAndTrain(0.75);
        List<DataSet> trainIter = testAndTrain.getTrain().batchBy(batchSize);
        List<DataSet> testIter = testAndTrain.getTest().batchBy(batchSize);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)
                .regularization(true)
                .l2(0.0001)
                //.learningRateDecayPolicy(LearningRatePolicy.Score)
                //.lrPolicyDecayRate(10)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                        .weightInit(WeightInit.XAVIER)
                        .activation("relu")
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .weightInit(WeightInit.XAVIER)
                        .activation("softmax").weightInit(WeightInit.XAVIER)
                        .nIn(numHiddenNodes).nOut(numOutputs).build())
                .pretrain(false).backprop(true).build();


        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        //model.setListeners(Collections.singletonList((IterationListener) new ScoreIterationListener(1)));
        model.setListeners(new ScoreIterationListener(1));
        model.setListeners(new HistogramIterationListener(1));

        UiServer server = UiServer.getInstance();
        System.out.println("Started on port " + server.getPort());

        for (int n = 0; n < nEpochs; n++) {
            for(DataSet ds : trainIter)
                model.fit(ds);
        }

        System.out.println("Evaluate model....");
        Evaluation eval = new Evaluation(numOutputs);
        //while (testIter.hasNext()) {
        for(DataSet ds : testIter){
            DataSet t = ds;
            INDArray features = t.getFeatureMatrix();
            INDArray lables = t.getLabels();
            INDArray predicted = model.output(features, false);

            eval.eval(lables, predicted);
            System.out.println(predicted);
            System.out.println(lables);
        }

        //Print the evaluation statistics
        System.out.println(eval.stats());

        //for training data
        System.out.println("Evaluate model....(for train data)");
        eval = new Evaluation(numOutputs);
        for(DataSet ds : trainIter){
            DataSet t = ds;
            INDArray features = t.getFeatureMatrix();
            INDArray lables = t.getLabels();
            INDArray predicted = model.output(features, false);

            eval.eval(lables, predicted);
        }

        //Print the evaluation statistics
        System.out.println(eval.stats());

    }


}
