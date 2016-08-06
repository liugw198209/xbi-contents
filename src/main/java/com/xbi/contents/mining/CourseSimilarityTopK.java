package com.xbi.contents.mining;

import com.xbi.contents.mining.tools.CourseVector;
import com.xbi.contents.mining.tools.DBManager;
import org.apache.commons.collections.FastHashMap;
import org.apache.commons.math3.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.lang.Math.min;

/**
 * Created by usr0101862 on 2016/08/06.
 */
public class CourseSimilarityTopK {

    private static final int TOP_K = 50;
    private static final int THREAD_THROUGHPUT = 200;

    static List<CourseVector> loadCourseVectorFromDB(){
        String query = "select course_id, vectors from le_scourse_vector limit 100000";
        List<CourseVector> rltVectors = new ArrayList<CourseVector>();

        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;

        try {
            connection = DBManager.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            rs = statement.executeQuery(query);
            while (rs.next()) {
                String docId = rs.getString(1);

                String vector = rs.getString(2);
                String []splits = vector.split(" ");
                INDArray fv = Nd4j.zeros(splits.length);
                int i=0;
                for(String s : splits){
                    fv.putScalar(i++, Float.parseFloat(s));
                }

                CourseVector cv = new CourseVector(docId, fv);
                rltVectors.add(cv);
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

            return rltVectors;
        }
    }

    static Map<String, List<Pair<String, Double>>> topKMap = null;
    static List<CourseVector> courses = null;
    public static void main(String[] args) throws Exception {
        courses = loadCourseVectorFromDB();
        topKMap = Collections.synchronizedMap(new FastHashMap(courses.size()));

        Executor ex = Executors.newCachedThreadPool();

        int start = 0;
        int end = 0;
        for(int th = 0; end < courses.size(); th++){
            start = THREAD_THROUGHPUT * th;
            end = min(start + THREAD_THROUGHPUT, courses.size());

            ComputeSimilarity rn = new ComputeSimilarity(start, end);
            ex.execute(rn);
        }

        saveTopKSimilarity();
    }

    static void saveTopKSimilarity(){
        String query = "INSERT INTO le_scourse_similarity_topk(course_id, pair_course_id, similarity, similarity_rank, create_datetime, update_datetime) VALUES (?, ?, ?, ?, now(), now())";

        Connection connection = null;

        try{
            connection = DBManager.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);

            int cnt = 0;
            for(Map.Entry e : topKMap.entrySet()){
                String courseId = (String) e.getKey();
                List<Pair<String, Double>> topK = (List<Pair<String, Double>>) e.getValue();

                int rank = 0;
                for(Pair<String, Double> p : topK){
                    ps.setString(1, courseId);
                    ps.setString(2, p.getKey()); //pair_course_id
                    ps.setDouble(3, p.getValue()); //similarity
                    ps.setInt(4, ++rank);

                    cnt++;
                    ps.addBatch();

                    if (cnt % 1000 == 0 ) {
                        ps.executeBatch(); // Execute every 1000 items.
                    }
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

    static class ComputeSimilarity implements Runnable{
        private int start;
        private int end;

        public ComputeSimilarity(int start, int end) {
            this.start = start;
            this.end = end;
        }

        Pair<String, Double> getMin(List<Pair<String, Double>> topK){
            return Collections.min(topK, new Comparator<Pair<String, Double>>() {
                @Override
                public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
        }

        @Override
        public void run() {
            System.out.println("Processing Courses start: " + start + ", end: " + end);

            for(int i=start; i<end; i++){
                List<Pair<String, Double>> topK = new ArrayList<>(TOP_K);
                Pair<String, Double> minSim = null;
                for(CourseVector c : courses){
                    Double sim = Transforms.cosineSim(courses.get(i).getVectors(), c.getVectors());
                    if(topK.size() < TOP_K){
                        topK.add(new Pair<String, Double>(c.getCourseId(), sim));
                    }
                    else {
                        if(minSim == null){
                            minSim = getMin(topK);
                        }

                        if(sim > minSim.getValue()){
                            topK.remove(minSim);
                            topK.add(new Pair<String, Double>(c.getCourseId(), sim));

                            minSim = getMin(topK);
                        }
                    }
                }

                Collections.sort(topK, new Comparator<Pair<String, Double>>() {
                    @Override
                    public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                        return o2.getValue().compareTo(o1.getValue());
                    }
                });

                topKMap.put(courses.get(i).getCourseId(), topK);
            }

            System.out.println("Finished Courses start: " + start + ", end: " + end);
        }
    }

}
