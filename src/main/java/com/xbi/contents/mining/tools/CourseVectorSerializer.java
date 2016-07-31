package com.xbi.contents.mining.tools;

import org.canova.api.split.FileSplit;
import org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.DataSetIterator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by usr0101862 on 2016/07/30.
 */
public class CourseVectorSerializer {
    //public int batchSize = 32;
    public static String inputFilePath = "course2vec.txt"; //"src/main/resources/doc/doc.train";
    private static HashMap<String, Integer> labelIds = null;
    protected static AtomicInteger IDG = new AtomicInteger(0);
    private static DataSetIterator fullDataSet = null;

    public static void main(String[] args) throws Exception {
        new CourseVectorSerializer().loadCourseVectors();
    }

    public static DataSetIterator loadCourseVectors(){
        List<DocItem> labeledDocs = LoadDataFromMysql.loadCourses("select page_id, description, title1, category1, url from le_scourse where title1 is not null and category1 is not null and length(category1)>0");

        HashMap<String, Integer> labeledDocMap = new HashMap<>(labeledDocs.size());
        labelIds = new HashMap<>();

        for(DocItem d : labeledDocs){
            if(!d.getDocCategory().isEmpty() && !getLabelIds().containsKey(d.getDocCategory()))
                getLabelIds().put(d.getDocCategory(), IDG.getAndIncrement());
            labeledDocMap.put(d.getDocId(), getLabelIds().get(d.getDocCategory()));
        }

        CourseVectorRecordReader rr = new CourseVectorRecordReader(0, " ");
        //RecordReader rr = new CSVRecordReader(0, " ");

        try {
            rr.initialize(new FileSplit(new File(inputFilePath)), labeledDocMap);
            //rr.initialize(new FileSplit(new File(inputFilePath)));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int examples = 0;
        while(rr.hasNext()){
            examples += 1;
            rr.next();
        }

        rr.reset();

        System.out.println("totalExamples: " + examples);
        int labelNum = getLabelIds().size();
        fullDataSet = new RecordReaderDataSetIterator(rr, examples, -1, labelNum);

        return fullDataSet;
    }

    public static HashMap<String, Integer> getLabelIds() {
        return labelIds;
    }

    public static void setLabelIds(HashMap<String, Integer> labelIds) {
        CourseVectorSerializer.labelIds = labelIds;
    }
}
