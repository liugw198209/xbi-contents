package com.xbi.contents.mining;

import com.xbi.contents.mining.tools.DocItem;
import com.xbi.contents.mining.tools.JapaneseTokenizerFactory;
import com.xbi.contents.mining.tools.LoadDataFromDB;
import com.xbi.contents.mining.tools.RowLabelAwareIterator;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.InMemoryLookupCache;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This is example code for dl4j ParagraphVectors implementation. In this example we build distributed representation of all sentences present in training corpus.
 * However, you still use it for training on labelled documents, using sets of LabelledDocument and LabelAwareIterator implementation.
 *
 * *************************************************************************************************
 * PLEASE NOTE: THIS EXAMPLE REQUIRES DL4J/ND4J VERSIONS >= rc3.8 TO COMPILE SUCCESSFULLY
 * *************************************************************************************************
 *
 * @author Guangwen Liu
 */
public class Article2VectorTraining {

    private static final Logger log = LoggerFactory.getLogger(Article2VectorTraining.class);

    public static void main(String[] args) throws Exception {
        List<DocItem> rs = LoadDataFromDB.loadDataFromSqlite(null, null);
        LabelAwareIterator iterator = new RowLabelAwareIterator.Builder()
                .setRowSet(rs)
                .build();

        InMemoryLookupCache cache = new InMemoryLookupCache();

        TokenizerFactory t = new JapaneseTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

        /*
             if you don't have LabelAwareIterator handy, you can use synchronized labels generator
              it will be used to label each document/sequence/line with it's own label.

              But if you have LabelAwareIterator ready, you can can provide it, for your in-house labels
        */

        ParagraphVectors vec = new ParagraphVectors.Builder()
                .minWordFrequency(1)
                .iterations(3)
                .epochs(1)
                .layerSize(100)
                .learningRate(0.025)
                .windowSize(5)
                .iterate(iterator)
                .trainWordVectors(false)
                .vocabCache(cache)
                .tokenizerFactory(t)
                .sampling(0)
                .build();

        vec.fit();

        double similarity1 = vec.similarity("DOC_9835", "DOC_12492");
        log.info("9835/12492 similarity: " + similarity1);

    }
}
