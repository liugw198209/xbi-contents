package com.xbi.contents.mining.tools;

import org.canova.api.records.reader.RecordReader;
import org.canova.api.records.reader.impl.CSVRecordReader;
import org.canova.api.split.FileSplit;
import org.canova.api.writable.Writable;
import org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.DataSetIterator;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
        saveCourseVectorsToDB("course2vec.txt");
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

    public static void saveCourseVectorsToDB(String filePath){
        String query = "INSERT INTO le_scourse_vector(course_id, create_datetime, update_datetime, vectors) VALUES (?, now(), now(), ?)";

        RecordReader rr = new CSVRecordReader(0, " ");

        try {
            rr.initialize(new FileSplit(new File(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Connection connection = null;

        try{
            connection = DBManager.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);

            int cnt = 0;
            while(rr.hasNext()){

                int col = 0;
                StringBuilder sb = new StringBuilder();
                for(Writable w : rr.next()){
                    if(col == 1) ps.setLong(1, w.toLong());
                    if(col > 1) sb.append(String.format("%.8f ", w.toDouble()));
                    col++;
                }
                if(col > 1) ps.setString(2, sb.substring(0, sb.length()-1));
                cnt++;

                ps.addBatch();

                if (cnt % 1000 == 0 ) {
                    ps.executeBatch(); // Execute every 1000 items.
                }
            }
            if(cnt % 1000 != 0)
                ps.executeBatch();

        } catch (Exception ex){
            ex.printStackTrace();
        }
        finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
