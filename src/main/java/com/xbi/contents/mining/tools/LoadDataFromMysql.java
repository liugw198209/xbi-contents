package com.xbi.contents.mining.tools;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Guangwen Liu on 2016/07/09.
 */
public class LoadDataFromMysql {
    public static String sqlDefault = "select page_id, description from le_scourse limit 100";
    private static Pattern ptn = Pattern.compile("\\s{2,}");

    public static List<DocItem> loadCourses(String sql){
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        String query = LoadDataFromMysql.sqlDefault;

        List<DocItem> rowSet = new ArrayList<DocItem>();

        try {
            if(sql != null) query = sql;

            connection = DBManager.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            //statement = connection.createStatement();

            rs = statement.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            int i = 0;
            while (rs.next()) {
                Matcher m = ptn.matcher(rs.getString(2));
                String filterContent = m.replaceAll(" ");
                //String filterContent = rs.getString(2);
                String docId = rs.getString(1);

                String docTitle = columnsNumber == 3 ? rs.getString(3) : null;

                if(i < 10){
                    Integer len = Math.min(filterContent.length(), 100);
                    System.out.println(docId + "," + filterContent.substring(0, len));
                }
                i++;

                rowSet.add(new DocItem(docId, filterContent, docTitle));
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

            return rowSet;
        }
    }

    public static void main(String[] args) throws SQLException {
        Connection con = DBManager.getConnection();

        String test = "abcr\r\n   \r\na    bd e\r\n    \r\n";
        Matcher m = ptn.matcher(test);
        String filterContent = m.replaceAll("");

        try{
            String selectSql = "select * from le_scourse limit 10";
            Statement stmt = con.createStatement();
            ResultSet result = stmt.executeQuery(selectSql);
            while (result.next()) {
                System.out.println(result.getString("url"));
                System.out.println(result.getString("description"));
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if(con != null && !con.isClosed())
                con.close();
        }
    }
}
