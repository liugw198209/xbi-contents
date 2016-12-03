package com.xbi.contents.mining.tools;

import lombok.NonNull;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.documentiterator.LabelsSource;

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
 * @author Guangwen Liu
 */
public class RowLabelAwareIterator implements LabelAwareIterator {
    protected List<DocItem> rowSet;
    protected AtomicInteger position = new AtomicInteger(0);
    protected LabelsSource labelsSource;

    /*
        Please keep this method protected, it's used in tests
     */
    protected RowLabelAwareIterator() {

    }

    protected RowLabelAwareIterator(@NonNull List rs, @NonNull LabelsSource source) {
        this.rowSet = rs;
        this.labelsSource = source;
    }

    @Override
    public boolean hasNextDocument() {
        return(this.position.get() < this.rowSet.size());
    }

    @Override
    public LabelledDocument nextDocument() {

        try {
            DocItem item = rowSet.get(position.getAndIncrement());
            String label = item.getDocId();
            String content = item.getDocContent();

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
        position.set(0);
    }

    @Override
    public boolean hasNext() {
        return hasNextDocument();
    }

    @Override
    public LabelledDocument next() {
        return nextDocument();
    }

    @Override
    public void remove() {
        // no-op
    }

    @Override
    public void shutdown() {
        // no-op
    }

    @Override
    public LabelsSource getLabelsSource() {
        return labelsSource;
    }

    public static class Builder {
        protected List<DocItem> rowSet = null;

        public Builder() {

        }

        /**
         * Root folder for labels -> documents.
         * Each subfolder name will be presented as label, and contents of this folder will be represented as LabelledDocument, with label attached
         *
         * @param rs RowSet to be scanned for docId and docContent
         * @return
         */
        public Builder setRowSet(@NonNull List rs) {
            this.rowSet = rs;
            return this;
        }

        public RowLabelAwareIterator build() {
            // search for all files in all folders provided
            List<String> labels = new ArrayList<>();

            for (DocItem item : rowSet) {
                String label = item.getDocId();
                labels.add(label);
            }

            LabelsSource source = new LabelsSource(labels);
            RowLabelAwareIterator iterator = new RowLabelAwareIterator(rowSet, source);

            return iterator;
        }
    }

    public static void main(String[] args) {
        List<DocItem> rs = LoadDataFromDB.loadDataFromSqlite(null, null);
        LabelAwareIterator iterator = new RowLabelAwareIterator.Builder()
                .setRowSet(rs)
                .build();

        assert(iterator.hasNextDocument());
        assert(iterator.nextDocument() != null);
    }
}