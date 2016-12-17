package com.xbi.contents.mining;

import com.xbi.contents.mining.tools.CourseVectorSerializer;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.saver.InMemoryModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
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
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.weights.HistogramIterationListener;
import org.joda.time.DateTime;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Guangwen Liu on 2016/07/02.
 */
public class CourseClassifier {
    static         int batchSize = 64;

    static boolean isPredict = true;

    static int seed = 123;
    static double learningRate = 0.005;
    static int nMaxEpochs = 500;

    static int numInputs = 100;
    static int numOutputs = 9;
    static int numHiddenNodes = 200;


    public static void main(String[] args) throws Exception {

        if(isPredict) {
            MultiLayerNetwork bestModel = train(320);
            predict(bestModel, CourseVectorSerializer.inputFilePath, numOutputs);
        }
        else{
            trainAndTest();
        }
    }

    static void trainAndTest() {
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

        //allData.next().shuffle();
        SplitTestAndTrain testAndTrain = allData.next().splitTestAndTrain(0.90);
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

//        UIServer server = UIServer.getInstance();
//        System.out.println("Started on port " + server.getPort());

//        for (int n = 0; n < nEpochs; n++) {
//            for(DataSet ds : trainIter)
//                model.fit(ds);
//        }

        EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
                .epochTerminationConditions(new MaxEpochsTerminationCondition(nMaxEpochs))
                .iterationTerminationConditions(new MaxTimeIterationTerminationCondition(20, TimeUnit.MINUTES))
                .scoreCalculator(new DataSetLossCalculator(new ListDataSetIterator(testIter), true))
                .evaluateEveryNEpochs(5)
                .modelSaver(new InMemoryModelSaver<>())
                .build();

        EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf,conf, new ListDataSetIterator(trainIter, batchSize));

//Conduct early stopping training:
        EarlyStoppingResult result = trainer.fit();

//Print out the results:
        System.out.println("Termination reason: " + result.getTerminationReason());
        System.out.println("Termination details: " + result.getTerminationDetails());
        System.out.println("Total epochs: " + result.getTotalEpochs());
        System.out.println("Best epoch number: " + result.getBestModelEpoch());
        System.out.println("Score at best epoch: " + result.getBestModelScore());

//Get the best model:
        MultiLayerNetwork bestModel = (MultiLayerNetwork) result.getBestModel();

        System.out.println("For test data");
        evalModel(bestModel, numOutputs, testIter, true);

        System.out.println("For train data");
        evalModel(bestModel, numOutputs, trainIter, false);


        System.out.println("Finished: " + DateTime.now());
    }

    static MultiLayerNetwork train(int nEpochs){
        DataSetIterator allData = CourseVectorSerializer.loadCourseVectors();
        numOutputs = CourseVectorSerializer.getLabelIds().size();

        List<DataSet> trainIter = allData.next().batchBy(batchSize);

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

        UIServer server = UIServer.getInstance();
        System.out.println("Started on port " + server.getPort());

        for (int n = 0; n < nEpochs; n++) {
            for(DataSet ds : trainIter)
                model.fit(ds);
        }

        System.out.println("Finished: " + DateTime.now());

        return model;
    }

    static void predict(MultiLayerNetwork model, String fileInput, int numOutputs) throws Exception{
        DataSetIterator allData = CourseVectorSerializer.loadCourseVectors(false);

        while(allData.hasNext()){
            DataSet t = allData.next();
            INDArray features = t.getFeatureMatrix();
            INDArray predicted = model.output(features, false);

            //System.out.println(predicted);
            CourseVectorSerializer.saveCategoryVectorsToDB(predicted);
        }
    }

    static void evalModel(MultiLayerNetwork model, int numOutputs, List<DataSet> testIter, boolean isPrintDetail){
        System.out.println("Evaluate model....");
        Evaluation eval = new Evaluation(numOutputs);
        //while (testIter.hasNext()) {
        for(DataSet ds : testIter){
            DataSet t = ds;
            INDArray features = t.getFeatureMatrix();
            INDArray lables = t.getLabels();
            INDArray predicted = model.output(features, false);

            eval.eval(lables, predicted);

            if(isPrintDetail){
                System.out.println(predicted);
                System.out.println(lables);
            }
        }

        //Print the evaluation statistics
        System.out.println(eval.stats());
    }

}
