package com.xbi.contents.mining.tools;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Guangwen Liu on 2016/06/05.
 */
public class LoadDataFromDB {
    public static String sqlLiteConnString = "jdbc:sqlite:news.db";
    public static String sqlDefault = "select id, post_content from xb_corpus";

    private static Pattern ptn = Pattern.compile("\\[p id=\"\\d+\"\\]");

    public static void main(String[] args) throws Exception {
        List<DocItem> rs = LoadDataFromDB.loadDataFromSqlite(null, null);
        assert(rs.size() > 0);
    }

    public static List<DocItem> loadDataFromMysql(String sql){
        return LoadDataFromMysql.loadCourses(sql);
    }

    public static List<DocItem> loadDataFromSqlite(String connString, String sql){
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        String query = LoadDataFromDB.sqlDefault;
        String sqliteConn = LoadDataFromDB.sqlLiteConnString;
        List<DocItem> rowSet = new ArrayList<DocItem>();

        try {
            Class.forName("org.sqlite.JDBC");

            if(sql != null) query = sql;
            if(connString != null) sqliteConn = connString;

            connection = DriverManager.getConnection(sqliteConn);
            //statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            statement = connection.createStatement();

            rs = statement.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            int i = 0;
            while (rs.next()) {
                Matcher m = ptn.matcher(rs.getString(2));
                String filterContent = m.replaceAll("");
                String postId = rs.getString(1);

                String postTitle = columnsNumber == 3 ? rs.getString(3) : null;

                if(i < 10){
                    Integer len = Math.min(filterContent.length(), 100);
                    System.out.println(postId + "," + filterContent.substring(0, len));
                }
                i++;

                rowSet.add(new DocItem(postId, filterContent, postTitle));
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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

            return rowSet;
        }
    }
}
