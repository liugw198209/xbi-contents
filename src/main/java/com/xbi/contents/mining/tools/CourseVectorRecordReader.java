package com.xbi.contents.mining.tools;


import org.datavec.api.conf.Configuration;
import org.datavec.api.records.reader.impl.LineRecordReader;
import org.datavec.api.split.InputSplit;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by usr0101862 on 2016/07/02.
 */
public class CourseVectorRecordReader extends LineRecordReader {

    private boolean skippedLines;
    private int skipNumLines;
    private String delimiter;
    public static final String SKIP_NUM_LINES;
    public static final String DELIMITER;
    private HashMap<String, Integer> labeledDocs = null;

    private ArrayList buffered = new ArrayList();
    private int currIndex = 0;

    public CourseVectorRecordReader(int skipNumLines) {
        this(skipNumLines, ",");
    }

    public CourseVectorRecordReader(int skipNumLines, String delimiter) {
        this.skippedLines = false;
        this.skipNumLines = 0;
        this.skipNumLines = skipNumLines;
        this.delimiter = delimiter;
    }

    public CourseVectorRecordReader() {
        this(0, ",");
    }

    public HashMap<String, Integer> getLabeledDocs() {
        return labeledDocs;
    }

    public void initialize(Configuration conf, InputSplit split) throws IOException, InterruptedException {
        super.initialize(conf, split);
        this.skipNumLines = conf.getInt(SKIP_NUM_LINES, this.skipNumLines);
        this.delimiter = conf.get(DELIMITER, ",");
    }

    public void initialize(InputSplit inputSplit, HashMap<String, Integer> labels) throws IOException, InterruptedException {
        super.initialize(inputSplit);
        labeledDocs = labels;

        while(super.hasNext()) {
            Text var9 = (Text) super.next().iterator().next();
            String val = var9.toString();
            String[] split = val.split(this.delimiter, -1);
            String[] var5 = split;
            int var6 = split.length;

            //if(var6 > 1 && labeledDocs != null){
            String type = var5[0];
            String id = var5[1];

            if (type.equals("L") && labeledDocs.containsKey(id)) {
                ArrayList ret = new ArrayList();
                System.out.println("id=" + id);

                for (int var7 = 2; var7 < var6; ++var7) {
                    String s = var5[var7];
                    ret.add(new Text(s));
                }

                //add label
                ret.add(new Text(labeledDocs.get(id).toString()));

                buffered.add(ret);
            }
        }
    }

    synchronized public List<Writable> next() {
        if(currIndex < buffered.size())
            return (List<Writable>) buffered.get(currIndex++);
        else
            return new ArrayList<>();
    }

    @Override
    synchronized public boolean hasNext(){
        return this.currIndex < buffered.size();
    }

    @Override
    synchronized public void reset(){
        this.currIndex = 0;
    }

    /*public List<Writable> next() {
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
        String[] var5 = split;
        int var6 = split.length;

        //if(var6 > 1 && labeledDocs != null){
            String type = var5[0];
            String id = var5[1];

            if(type.equals("L") && labeledDocs.containsKey(id)){
                ArrayList ret = new ArrayList();
                System.out.println("id=" + id);

                for(int var7 = 2; var7 < var6; ++var7) {
                    String s = var5[var7];
                    ret.add(new Text(s));
                }

                //add label
                ret.add(new Text(labeledDocs.get(id).toString()));

                return ret;
            }
        //}

        if(!this.hasNext()) {
            return new ArrayList();
        }
        else{
            return this.next();
        }
    }*/

    public List<Writable> record(URI uri, DataInputStream dataInputStream) throws IOException {
        throw new UnsupportedOperationException("Reading CSV data from DataInputStream not yet implemented");
    }

    static {
        SKIP_NUM_LINES = NAME_SPACE + ".skipnumlines";
        DELIMITER = NAME_SPACE + ".delimiter";
    }
}
