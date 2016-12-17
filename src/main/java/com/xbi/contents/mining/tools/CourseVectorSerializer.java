package com.xbi.contents.mining.tools;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.writable.Writable;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by usr0101862 on 2016/07/30.
 */
public class CourseVectorSerializer {
    //public int batchSize = 32;
    public static String inputFilePath = "course2vec.txt.dm"; //"src/main/resources/doc/doc.train";
    private static HashMap<String, Integer> labelIds = null;
    protected static AtomicInteger IDG = new AtomicInteger(0);
    private static DataSetIterator fullDataSet = null;

    public static void main(String[] args) throws Exception {
        saveCourseVectorsToDB("course2vec.txt");
    }

    public static DataSetIterator loadCourseVectors() {
        return loadCourseVectors(true);
    }

    public static DataSetIterator loadCourseVectors(Boolean hasLables) {
        //List<DocItem> labeledDocs = LoadDataFromMysql.loadCourses("select page_id, description, title1, category1, url from le_scourse where title1 is not null and category1 is not null and length(category1)>0 order by rand()");

        HashMap<String, Integer> labeledDocMap = null;
        if (hasLables) {
            List<DocItem> labeledDocs = LoadDataFromMysql.loadCourses("select page_id, description, title1, category2, url from le_scourse where title1 is not null and category2 is not null and length(category2)>0 order by rand()");

            labeledDocMap = new HashMap<>(labeledDocs.size());
            labelIds = new HashMap<>();

            for (DocItem d : labeledDocs) {
                if (!d.getDocCategory().isEmpty() && !getLabelIds().containsKey(d.getDocCategory()))
                    getLabelIds().put(d.getDocCategory(), IDG.getAndIncrement());
                labeledDocMap.put(d.getDocId(), getLabelIds().get(d.getDocCategory()));
            }
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
        while (rr.hasNext()) {
            examples += 1;
            rr.next();
        }

        rr.reset();

        System.out.println("totalExamples: " + examples);

        if (hasLables) {
            int labelNum = getLabelIds().size();
            fullDataSet = new RecordReaderDataSetIterator(rr, examples, -1, labelNum);
        } else {
            fullDataSet = new RecordReaderDataSetIterator(rr, examples, -1, 0);
        }

        return fullDataSet;
    }

    public static HashMap<String, Integer> getLabelIds() {
        return labelIds;
    }

    public static void setLabelIds(HashMap<String, Integer> labelIds) {
        CourseVectorSerializer.labelIds = labelIds;
    }

    public static HashMap<Integer, String> getCatIds(HashMap<String, Integer> labelIds) {
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        String query = "SELECT cat_id, cat_name FROM le_category";

        HashMap<Integer, String> catIds = new HashMap<>();

        try {
            connection = DBManager.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            rs = statement.executeQuery(query);
            while (rs.next()) {
                String catId = rs.getString(1);
                String catName = rs.getString(2);

                if (labelIds.containsKey(catName)) {
                    catIds.put(labelIds.get(catName), catId);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return catIds;
    }

    public static void saveCategoryVectorsToDB(INDArray predicted){
        HashMap catIds = getCatIds(getLabelIds());

        String query = "INSERT INTO le_course_category(course_id, cat_id, ms_score, create_datetime) VALUES (?, ?, ?, now())";

        int rows = predicted.shape()[0];
        int len = predicted.shape()[1];

        RecordReader rr = new CSVRecordReader(0, " ");

        try {
            rr.initialize(new FileSplit(new File(inputFilePath)));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Connection connection = null;

        try {
            connection = DBManager.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);

            int cnt = 0;
            while (rr.hasNext()) {

                int col = 0;
                StringBuilder sb = new StringBuilder();

                Long cid = -1L;
                int i = 0;
                for (Writable w : rr.next()) {
                    if(i == 1) {
                        cid = w.toLong();
                        break;
                    }
                    i++;
                }
                for(col=0; col<len; col++){
                    ps.setLong(1, cid);
                    ps.setString(2, (String)catIds.get(col));
                    ps.setDouble(3, predicted.getDouble(cnt, col));
                    ps.addBatch();
                }
                cnt++;

                if (cnt % 1000 == 0) {
                    ps.executeBatch(); // Execute every 100 items.
                }
            }
            if (cnt % 1000 != 0)
                ps.executeBatch();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveCourseVectorsToDB(String filePath) {
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

        try {
            connection = DBManager.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);

            int cnt = 0;
            while (rr.hasNext()) {

                int col = 0;
                StringBuilder sb = new StringBuilder();
                for (Writable w : rr.next()) {
                    if (col == 1) ps.setLong(1, w.toLong());
                    if (col > 1) sb.append(String.format("%.8f ", w.toDouble()));
                    col++;
                }
                if (col > 1) ps.setString(2, sb.substring(0, sb.length() - 1));
                cnt++;

                ps.addBatch();

                if (cnt % 1000 == 0) {
                    ps.executeBatch(); // Execute every 1000 items.
                }
            }
            if (cnt % 1000 != 0)
                ps.executeBatch();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
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
