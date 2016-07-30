package com.xbi.contents.mining;

import com.xbi.contents.mining.tools.DocItem;
import com.xbi.contents.mining.tools.JapaneseTokenizerFactory;
import com.xbi.contents.mining.tools.LoadDataFromDB;
import com.xbi.contents.mining.tools.RowLabelAwareIterator;
import lombok.NonNull;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.InMemoryLookupCache;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
public class Course2VectorTraining {

    private static final Logger log = LoggerFactory.getLogger(Course2VectorTraining.class);
    private static final String whitespaceReplacement = "_Az92_";

    public static void main(String[] args) throws Exception {
        String inputSql = "select page_id, description from le_scourse where length(description) > 50 limit 100000";

        List<DocItem> rs = LoadDataFromDB.loadDataFromMysql(inputSql);
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

        //UiServer server = UiServer.getInstance();
        //System.out.println("Started on port " + server.getPort());

        ParagraphVectors vec = new ParagraphVectors.Builder()
                .minWordFrequency(3)
                .iterations(5)
                .epochs(100)
                .layerSize(100)
                .learningRate(0.025)
                .windowSize(9)
                .iterate(iterator)
                .trainWordVectors(false)
                .vocabCache(cache)
                .tokenizerFactory(t)
                .sampling(0)
                .build();

        vec.fit();

        log.info("Writing course vectors to text file....");

        // Write word vectors
        writeWordVectors(vec, "course2vec.txt");
        //WordVectorSerializer.writeFullModel(vec, "fullmodel.txt");

        /*
        iterator.reset();
        LabelledDocument doc1 = iterator.nextDocument();
        LabelledDocument doc2 = iterator.nextDocument();

        //docId is a word. vec contain all words and docIds
        //Two types of similarity: (1) trained vectors if given labels (docId), (2) mean vectors if given doc contents
        double sim1 = vec.similarityToLabel(doc1, doc1.getLabel());
        double sim2 = vec.similarity(doc1.getLabel(), doc2.getLabel());
        double[] sim3 = vec.getWordVector(doc1.getLabel());
        //double sim4 = vec.similarityToLabel(doc1.getReferencedContent(), doc2.getLabel());
        double sim5 = vec.similarityToLabel(doc1.getContent(), doc2.getLabel());
        double sim6 = vec.similarityToLabel(doc2.getLabel(), doc2.getLabel());

        log.info(sim1 + "," + sim2 + "," + sim5 + "," + sim6);

        */
    }

    public static void writeWordVectors(@NonNull ParagraphVectors vectors, @NonNull String path) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            writeWordVectors(vectors, fos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeWordVectors(ParagraphVectors vectors, OutputStream stream) {

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"))) {
        /*
            This method acts similary to w2v csv serialization, except of additional tag for labels
         */

            VocabCache<VocabWord> vocabCache = vectors.getVocab();
            for (VocabWord word : vocabCache.vocabWords()) {
                if(word.isLabel()) { // Paragraph Vector only
                    StringBuilder builder = new StringBuilder();

                    builder.append(word.isLabel() ? "L" : "E").append(" ");
                    builder.append(word.getLabel().replaceAll(" ", whitespaceReplacement)).append(" ");

                    INDArray vector = vectors.getWordVectorMatrix(word.getLabel());
                    for (int j = 0; j < vector.length(); j++) {
                        builder.append(vector.getDouble(j));
                        if (j < vector.length() - 1) {
                            builder.append(" ");
                        }
                    }

                    writer.write(builder.append("\n").toString());
                }
            }

            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
