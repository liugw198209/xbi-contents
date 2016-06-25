package com.xbi.contents.mining;

import com.xbi.contents.mining.tools.DocItem;
import com.xbi.contents.mining.tools.JapaneseTokenizerFactory;
import com.xbi.contents.mining.tools.LoadDataFromDB;
import com.xbi.contents.mining.tools.RowIterator;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by agibsonccc on 10/9/14.
 */
public class Word2VecTraining {

    private static Logger log = LoggerFactory.getLogger(Word2VecTraining.class);

    public static void main(String[] args) throws Exception {

        String inputSql = "select id, post_content, post_title from xb_corpus";

        List<DocItem> rs = LoadDataFromDB.loadDataFromSqlite(null, inputSql);
        log.info("Load & Vectorize Sentences....");
        // Strip white space before and after for each line
        SentenceIterator iter = new RowIterator(rs);
        // Split on white spaces in the line to get words
        TokenizerFactory t = new JapaneseTokenizerFactory();
        //t.setTokenPreProcessor(new CommonPreprocessor());

        log.info("Building model....");
        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(3)
                .iterations(1)
                .layerSize(100)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

        log.info("Fitting Word2Vec model....");
        vec.fit();

        log.info("Writing word vectors to text file....");

        // Write word vectors
        WordVectorSerializer.writeWordVectors(vec, "word2vec.txt");


    }
}
