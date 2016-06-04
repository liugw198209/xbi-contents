package com.xbi.contents.mining.tools;

import lombok.NonNull;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.documentiterator.LabelsSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is simple filesystem-based LabelAware iterator.
 * It assumes that you have one or more folders organized in the following way:
 * 1st level subfolder: label name
 * 2nd level: bunch of documents for that label
 *
 * You can have as many label folders as you want, as well.
 *
 * Please note: as of DL4j 3.9 this iterator is available as part of DL4j codebase, so there's no need to use this implementation.
 *
 * @author raver119@gmail.com
 */
public class RowLabelAwareIterator implements LabelAwareIterator {
    protected ResultSet rowSet;
    protected AtomicInteger position = new AtomicInteger(1);
    protected LabelsSource labelsSource;

    /*
        Please keep this method protected, it's used in tests
     */
    protected RowLabelAwareIterator() {

    }

    protected RowLabelAwareIterator(@NonNull ResultSet rs, @NonNull LabelsSource source) {
        this.rowSet = rs;
        this.labelsSource = source;
    }

    @Override
    public boolean hasNextDocument() {
        try {
            return rowSet.absolute(position.get());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            return false;
        }
    }


    @Override
    public LabelledDocument nextDocument() {

        try {
            rowSet.absolute(position.getAndIncrement());
            String label = rowSet.getString(1);
            String content = rowSet.getString(2);

            LabelledDocument document = new LabelledDocument();

            document.setContent(content);
            document.setLabel(label);

            return document;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() {

        try {
            position.set(1); //Probably?
            rowSet.beforeFirst();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public LabelsSource getLabelsSource() {
        return labelsSource;
    }

    public static class Builder {
        protected ResultSet rowSet = null;

        public Builder() {

        }

        /**
         * Root folder for labels -> documents.
         * Each subfolder name will be presented as label, and contents of this folder will be represented as LabelledDocument, with label attached
         *
         * @param rs RowSet to be scanned for docId and docContent
         * @return
         */
        public Builder setRowSet(@NonNull ResultSet rs) {
            this.rowSet = rs;
            return this;
        }

        public RowLabelAwareIterator build() {
            // search for all files in all folders provided
            List<String> labels = new ArrayList<>();

            try {
                rowSet.beforeFirst();

                while(rowSet.next()){
                    String label = rowSet.getString(1);
                    labels.add(label);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            LabelsSource source = new LabelsSource(labels);
            RowLabelAwareIterator iterator = new RowLabelAwareIterator(rowSet, source);

            return iterator;
        }
    }

    public static void main(String[] args) {
        ResultSet rs = LoadDataFromDB.loadDataFromSqlite(null, null);
        LabelAwareIterator iterator = new RowLabelAwareIterator.Builder()
                .setRowSet(rs)
                .build();

        assert(iterator.hasNextDocument());
        assert(iterator.nextDocument() != null);
    }
}