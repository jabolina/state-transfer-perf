package org.infinispan;

import org.jgroups.util.Streamable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Metric implements Streamable {
    private static final long[] EMPTY = {};
    private long puts;
    private long gets;
    private long[] getHistograms;
    private long[] putHistograms;

    public Metric() { }

    public Metric(long puts, long[] putHistograms, long gets, long[] getHistograms) {
        this.puts = puts;
        this.gets = gets;
        this.getHistograms = getHistograms;
        this.putHistograms = putHistograms;
    }

    public static Metric empty() {
        return new Metric(0, EMPTY, 0, EMPTY);
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        out.writeLong(puts);
        out.writeLong(gets);
        writeHistograms(getHistograms, out);
        writeHistograms(putHistograms, out);
    }

    public long puts() {
        return puts;
    }

    public long putsAt(int index) {
        if (index < putHistograms.length)
            return putHistograms[index];

        return 0;
    }

    public long gets() {
        return gets;
    }

    public long getsAt(int index) {
        if (index < getHistograms.length)
            return getHistograms[index];

        return 0;
    }

    public long time() {
        return Math.max(getHistograms.length, putHistograms.length);
    }

    private static void writeHistograms(long[] histograms, DataOutput out) throws IOException {
        out.writeInt(histograms.length);
        for (long histogram : histograms) {
            out.writeLong(histogram);
        }
    }

    @Override
    public void readFrom(DataInput in) throws IOException {
        puts = in.readLong();
        gets = in.readLong();
        getHistograms = readHistograms(in);
        putHistograms = readHistograms(in);
    }

    public Metric add(Metric metric) {
        long[] putHistogramsCopy = new long[putHistograms.length];
        for (int i = 0; i < putHistograms.length && i < metric.putHistograms.length; i++) {
            putHistogramsCopy[i] = putHistograms[i] + metric.putHistograms[i];
        }

        long[] getHistogramsCopy = new long[getHistograms.length];
        for (int i = 0; i < getHistograms.length && i < metric.getHistograms.length; i++) {
            getHistogramsCopy[i] = getHistograms[i] + metric.getHistograms[i];
        }

        return new Metric(puts + metric.puts, putHistogramsCopy, gets + metric.gets, getHistogramsCopy);
    }

    public String summary() {
        return String.format("puts=%d / gets=%d", puts, gets);
    }

    private static long[] readHistograms(DataInput in) throws IOException {
        int length = in.readInt();
        long[] histograms = new long[length];
        for (int i = 0; i < length; i++) {
            histograms[i] = in.readLong();
        }

        return histograms;
    }
}
