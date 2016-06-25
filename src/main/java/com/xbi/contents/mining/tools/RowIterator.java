package com.xbi.contents.mining.tools;

import lombok.NonNull;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by usr0101862 on 2016/06/25.
 */
public class RowIterator implements SentenceIterator
{
    protected List<DocItem> rowSet;
    protected AtomicInteger position = new AtomicInteger(0);
    private SentencePreProcessor preProcessor;

    public RowIterator(@NonNull List rs) {
        this.rowSet = rs;
    }

    @Override
    public synchronized String nextSentence() {
        try {
            DocItem item = rowSet.get(position.getAndIncrement());

            String content = item.getDocContent();
            String title = item.getDocTitle();
            if(title != null && !title.isEmpty())
                content = title + "\n" + content;

            return (getPreProcessor() != null) ? this.getPreProcessor().preProcess(content) : content;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized boolean hasNext() {
        return(this.position.get() < this.rowSet.size());
    }

    @Override
    public synchronized void reset() {
        position.set(0);
    }

    @Override
    public void finish() {

    }

    @Override
    public SentencePreProcessor getPreProcessor() {
        return preProcessor;
    }

    @Override
    public void setPreProcessor(SentencePreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

}
