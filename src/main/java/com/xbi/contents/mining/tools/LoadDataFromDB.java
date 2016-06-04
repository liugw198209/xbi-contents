package com.xbi.contents.mining.tools;

import java.sql.*;

/**
 * Created by usr0101862 on 2016/06/05.
 */
public class LoadDataFromDB {
    public static String sqlLiteConnString = "jdbc:sqlite:news.db";
    public static String sqlDefault = "select id, post_content from xb_corpus";

    public static void main(String[] args) throws Exception {
        ResultSet rs = LoadDataFromDB.loadDataFromSqlite(null, null);
    }

    public static ResultSet loadDataFromSqlite(String connString, String sql){
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        String query = LoadDataFromDB.sqlDefault;
        String sqliteConn = LoadDataFromDB.sqlLiteConnString;

        try {
            Class.forName("org.sqlite.JDBC");

            if(sql != null) query = sql;
            if(connString != null) sqliteConn = connString;

            connection = DriverManager.getConnection(sqliteConn);
            statement = connection.createStatement();

            rs = statement.executeQuery(query);
            int i = 0;
            while (rs.next() && i < 10) {
                System.out.println(rs.getString(1) + "," + rs.getString(2).substring(0, 20));
                i++;
            }

            rs.beforeFirst();

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

            return rs;
        }
    }
}
