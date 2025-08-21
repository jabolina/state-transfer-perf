package org.infinispan;

import static org.jgroups.util.Util.printTime;

import org.jgroups.util.Streamable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;

import org.HdrHistogram.Histogram;

public class Metric implements Streamable {
    private long puts;
    private long gets;
    private Histogram[] getHistograms;
    private Histogram[] putHistograms;

    public Metric() { }

    public Metric(long puts, Histogram[] putHistograms, long gets, Histogram[] getHistograms) {
        this.puts = puts;
        this.gets = gets;
        this.getHistograms = getHistograms;
        this.putHistograms = putHistograms;
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

    public long gets() {
        return gets;
    }

    public long time() {
        return Math.max(getHistograms.length, putHistograms.length);
    }

    private static void writeHistograms(Histogram[] histograms, DataOutput out) throws IOException {
        out.writeInt(histograms.length);
        for (Histogram histogram : histograms) {
            int size = histogram.getEstimatedFootprintInBytes();
            ByteBuffer buffer = ByteBuffer.allocate(size);
            histogram.encodeIntoCompressedByteBuffer(buffer, 9);
            out.writeInt(buffer.position());
            out.write(buffer.array(), 0, buffer.position());
        }
    }

    @Override
    public void readFrom(DataInput in) throws IOException, ClassNotFoundException {
        puts = in.readLong();
        gets = in.readLong();

        try {
            getHistograms = readHistograms(in);
            putHistograms = readHistograms(in);
        } catch (DataFormatException e) {
            throw new IOException(e);
        }
    }

    public Metric add(Metric metric) {
        for (int i = 0; i < putHistograms.length && i < metric.putHistograms.length; i++) {
            putHistograms[i].add(metric.putHistograms[i]);
        }

        for (int i = 0; i < getHistograms.length && i < metric.getHistograms.length; i++) {
            getHistograms[i].add(metric.getHistograms[i]);
        }

        return new Metric(puts + metric.puts, putHistograms, gets + metric.gets, getHistograms);
    }

    public String summary() {
        Histogram writeSummary = reduce(putHistograms);
        Histogram readSummary = reduce(getHistograms);

        return String.format("get %s / %s / %s, put: %s / %s / %s",
                printTime(readSummary.getMinValue(), TimeUnit.NANOSECONDS),
                printTime(readSummary.getMean(), TimeUnit.NANOSECONDS),
                printTime(readSummary.getMaxValueAsDouble(), TimeUnit.NANOSECONDS),
                printTime(writeSummary.getMinValue(), TimeUnit.NANOSECONDS),
                printTime(writeSummary.getMean(), TimeUnit.NANOSECONDS),
                printTime(writeSummary.getMaxValueAsDouble(), TimeUnit.NANOSECONDS));
    }

    private static Histogram reduce(Histogram[] histograms) {
        Histogram identity = histograms[0].copy();
        return Arrays.stream(histograms)
                .reduce(identity, (acc, curr) -> {
                    if (curr != histograms[0]) acc.add(curr);
                    return acc;
                });
    }

    private Histogram[] readHistograms(DataInput in) throws IOException, DataFormatException {
        int length = in.readInt();
        Histogram[] histograms = new Histogram[length];
        for (int i = 0; i < length; i++) {
            int size = in.readInt();
            byte[] array = new byte[size];
            in.readFully(array);
            ByteBuffer buffer = ByteBuffer.wrap(array);
            histograms[i] = Histogram.decodeFromCompressedByteBuffer(buffer, 0);
        }

        return histograms;
    }
}
