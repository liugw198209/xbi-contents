package com.xbi.contents.mining.tools;

import org.canova.api.conf.Configuration;
import org.canova.api.io.data.Text;
import org.canova.api.records.reader.impl.LineRecordReader;
import org.canova.api.split.InputSplit;
import org.canova.api.writable.Writable;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by usr0101862 on 2016/07/02.
 */
public class ArticleVectorRecordReader extends LineRecordReader {

    private boolean skippedLines;
    private int skipNumLines;
    private String delimiter;
    public static final String SKIP_NUM_LINES;
    public static final String DELIMITER;

    public ArticleVectorRecordReader(int skipNumLines) {
        this(skipNumLines, ",");
    }

    public ArticleVectorRecordReader(int skipNumLines, String delimiter) {
        this.skippedLines = false;
        this.skipNumLines = 0;
        this.skipNumLines = skipNumLines;
        this.delimiter = delimiter;
    }

    public ArticleVectorRecordReader() {
        this(0, ",");
    }

    public void initialize(Configuration conf, InputSplit split) throws IOException, InterruptedException {
        super.initialize(conf, split);
        this.skipNumLines = conf.getInt(SKIP_NUM_LINES, this.skipNumLines);
        this.delimiter = conf.get(DELIMITER, ",");
    }

    public Collection<Writable> next() {
        if(!this.skippedLines && this.skipNumLines > 0) {
            for(int t = 0; t < this.skipNumLines; ++t) {
                if(!this.hasNext()) {
                    return new ArrayList();
                }

                super.next();
            }

            this.skippedLines = true;
        }

        Text var9 = (Text)super.next().iterator().next();
        String val = var9.toString();
        String[] split = val.split(this.delimiter, -1);
        ArrayList ret = new ArrayList();
        String[] var5 = split;
        int var6 = split.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            String s = var5[var7];
            ret.add(new Text(s));
        }

        return ret;
    }

    public Collection<Writable> record(URI uri, DataInputStream dataInputStream) throws IOException {
        throw new UnsupportedOperationException("Reading CSV data from DataInputStream not yet implemented");
    }

    static {
        SKIP_NUM_LINES = NAME_SPACE + ".skipnumlines";
        DELIMITER = NAME_SPACE + ".delimiter";
    }
}
