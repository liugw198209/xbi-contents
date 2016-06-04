package com.xbi.contents.mining.tools;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Guangwen Liu on 2016/06/05.
 */
public class LoadDataFromDB {
    public static String sqlLiteConnString = "jdbc:sqlite:news.db";
    public static String sqlDefault = "select id, post_content from xb_corpus";

    public static void main(String[] args) throws Exception {
        List<DocItem> rs = LoadDataFromDB.loadDataFromSqlite(null, null);
        assert(rs.size() > 0);
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
            int i = 0;
            while (rs.next()) {
                if(i < 10){
                    System.out.println(rs.getString(1) + "," + rs.getString(2).substring(0, 20));
                }
                i++;
                rowSet.add(new DocItem(rs.getString(1), rs.getString(2)));
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
