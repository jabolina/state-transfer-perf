package org.infinispan.control;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.ByteArrayDataInputStream;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.ResponseCollector;
import org.jgroups.util.Util;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.Agent;
import org.infinispan.Main;
import org.infinispan.Metric;

public class ControlHandler implements Receiver {
    private static final Logger LOG = LogManager.getLogger(ControlHandler.class);

    private final ExecutorService executor;
    private final JChannel channel;
    private final Main.Scaler.ControlSection configuration;
    private final Agent agent;
    private final Runnable completionListener;
    private final ResponseCollector<Metric> collector;

    public ControlHandler(JChannel channel, Agent agent, Main.Scaler.ControlSection configuration, Runnable completionListener) {
        this.channel = channel;
        this.agent = agent;
        this.configuration = configuration;
        this.completionListener = completionListener;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.collector = new ResponseCollector<>();
    }

    public void start() throws Exception {
        LOG.info("Connecting control channel");
        channel.setReceiver(this);
        channel.connect("scale-control");

//        if (configuration.isController()) {
//            viewAccepted(channel.view());
//        }
    }

    private void execute() {
        LOG.info("Starting test");
        agent.populate();

        LOG.info("Perform warmup phase");
        warmup();

        LOG.info("Perform load test");
        loadTest();

        LOG.info("Shutting cluster down");
        try {
            send(null, ProtocolStep.SHUTDOWN);
        } catch (Exception e) {
            LOG.error("Failed to send shutdown message", e);
        }
    }

    private void warmup() {
        collector.reset(channel.view().getMembers());

        try {
            send(null, ProtocolStep.WARMUP);
        } catch (Throwable t) {
            LOG.error("Failed to submit warmup", t);
            return;
        }

        boolean allResults = collector.waitForAllResponses(agent.configuration().getWarmupDuration().multipliedBy(2).toMillis());
        if (!allResults)
            LOG.warn("Missing result from members: {}", collector.getMissing());

        LOG.info("Received all metrics for warmup");
    }

    private void loadTest() {
        collector.reset(channel.view().getMembers());

        try {
            send(null, ProtocolStep.EXECUTE);
        } catch (Throwable t) {
            LOG.error("Failed to submit execute step", t);
            return;
        }

        boolean allResults = collector.waitForAllResponses(agent.configuration().getTestDuration().multipliedBy(2).toMillis());
        if (!allResults)
            LOG.warn("Missing results from members: {}", collector.getMissing());

        Metric overall = null;
        StringBuilder sb = new StringBuilder("Load test summary:").append(System.lineSeparator());

        long totalReqs = 0, totalTime = 0, longestTime = 0;
        for (Map.Entry<Address, Metric> entry : collector.getResults().entrySet()) {
            Address mbr = entry.getKey();
            Metric result = entry.getValue();

            if (overall == null)
                overall = result;

            if(result != null) {
                totalReqs += result.puts() + result.gets();
                totalTime += result.time();
                longestTime = Math.max(longestTime, result.time());
                sb.append(mbr).append(": ").append(result.summary()).append(System.lineSeparator());

                if (overall != result)
                    overall = overall.add(result);
            }
        }

        double reqSecNode = totalReqs / (totalTime / 1000f);
        double reqSecCluster = totalReqs / (longestTime / 1000f);
        double throughput = reqSecNode * agent.configuration().getMessageSize();
        String summary=String.format("Throughput: %,.0f reqs/sec/node (%s/sec) %,.0f reqs/sec/cluster%nTotal: %s",
                reqSecNode, Util.printBytes(throughput), reqSecCluster,
                overall != null ? overall.summary() : "");

        sb.append(System.lineSeparator()).append("\033[1m").append(summary).append("\033[0m").append(System.lineSeparator());
        LOG.info(sb);
    }

    private void send(Address dest, ProtocolStep step, Object ... args) throws Exception {
        if (args == null || args.length == 0) {
            channel.send(dest, new byte[]{ (byte) step.ordinal() });
            return;
        }

        ByteArrayDataOutputStream out = new ByteArrayDataOutputStream(512);
        out.write((byte) step.ordinal());
        for (Object arg : args)
            Util.objectToStream(arg, out);
        channel.send(dest, out.buffer(), 0, out.position());
    }

    @Override
    public void receive(Message msg) {
        try {
            receiveInternal(msg);
        } catch (Throwable t) {
            LOG.error("Failed handling message from {}", msg.src(), t);
        }
    }

    private void receiveInternal(Message msg) throws Throwable {
        byte[] buf = msg.getArray();
        byte type = buf[msg.getOffset()];
        ProtocolStep step = ProtocolStep.find(type);

        LOG.info("Received step {} from {}", step, msg.src());
        switch (step) {
            case WARMUP -> agent.warmup().whenComplete((m, t) -> {
                if (t != null)
                    LOG.error("Failed executing warmup", t);
                replyResult(msg.src(), m);
            });
            case EXECUTE -> agent.execute().whenComplete((m, t) -> {
                if (t != null)
                    LOG.error("Failed executing test", t);
                replyResult(msg.src(), m);
            });
            case STOP -> agent.stop();
            case RESULTS -> {
                ByteArrayDataInputStream in = new ByteArrayDataInputStream(buf, msg.getOffset() + 1, msg.getLength() - 1);
                Metric metric = Util.objectFromStream(in);
                collector.add(msg.src(), metric);
            }
            case SHUTDOWN -> {
                Util.close(channel);
                executor.close();
                completionListener.run();
            }
        }
    }

    private void replyResult(Address sender, Metric metric) {
        try {
            send(sender, ProtocolStep.RESULTS, metric);
        } catch (Exception e) {
            LOG.error("Failed to reply steps to {}", sender, e);
        }
    }

    @Override
    public void viewAccepted(View new_view) {
        LOG.info("Received view: {}", new_view);
        if (!configuration.isController()) return;

        if (new_view.size() < configuration.getClusterSize()) {
            LOG.info("View {}, expected cluster of {} before proceeding", new_view, configuration.getClusterSize());
            return;
        }

        LOG.info("Received view with minimum number of members ({}): {}", configuration.getClusterSize(), new_view);
        executor.submit(this::execute);
    }
}
