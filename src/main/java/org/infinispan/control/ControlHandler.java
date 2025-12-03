package org.infinispan.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.Agent;
import org.infinispan.Main;
import org.infinispan.Metric;
import org.infinispan.agent.AgentFactory;
import org.infinispan.util.AsyncProfilerWrapper;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.ByteArrayDataInputStream;
import org.jgroups.util.ByteArrayDataOutputStream;
import org.jgroups.util.ResponseCollector;
import org.jgroups.util.Util;

public class ControlHandler implements Receiver {
   private static final Logger LOG = LogManager.getLogger(ControlHandler.class);

   private final ExecutorService executor;
   private final JChannel channel;
   private final Main.Scaler.ControlSection configuration;
   private final Runnable completionListener;
   private final ResponseCollector<Metric> loadCollector;
   private final ResponseCollector<Long> scaleCollector;
   private final AgentFactory.Create factory;
   private Agent agent;

   public ControlHandler(JChannel channel, AgentFactory.Create factory, Main.Scaler.ControlSection configuration, Runnable completionListener) {
      this.channel = channel;
      this.factory = factory;
      this.configuration = configuration;
      this.completionListener = completionListener;
      this.executor = Executors.newVirtualThreadPerTaskExecutor();
      this.loadCollector = new ResponseCollector<>();
      this.scaleCollector = new ResponseCollector<>();
   }

   public void start() throws Exception {
      LOG.info("Connecting control channel");
      channel.setReceiver(this);
      channel.connect("scale-control");
   }

   private void execute() {
      LOG.info("Starting test");
      if (agent != null)
         throw new IllegalStateException("Agent is already in place");

      try {
         agent = factory.create();
      } catch (Throwable e) {
         LOG.error("Failed creating agent", e);
         throw new RuntimeException(e);
      }

      agent.init();
      LOG.info("Initial cluster scale to {}", configuration.getInitialSize());
      boolean fullScale = configuration.getInitialSize() < 0;

      if (fullScale) {
         agent.populate();
         scale(configuration.getClusterSize(), Duration.ofDays(1));
      } else {
         scale(configuration.getInitialSize(), Duration.ofDays(1));
         agent.populate();
      }

      if (configuration.isIncludeLoad()) {
         LOG.info("Perform load test");
         loadTest(ProtocolStep.WARMUP, agent.configuration().getWarmupDuration().multipliedBy(2));
         loadTest(ProtocolStep.EXECUTE, agent.configuration().getTestDuration().multipliedBy(2));
      }

      if (!fullScale) {
         scale(configuration.getScaleToSize(), Duration.ofDays(1));
      }

      agent.showOccupancy();
//      try {
//         Thread.sleep(300_000);
//      } catch (InterruptedException e) {
//         Thread.currentThread().interrupt();
//      }

      LOG.info("Shutting cluster down");
      try {
         send(null, ProtocolStep.SHUTDOWN);
      } catch (Exception e) {
         LOG.error("Failed to send shutdown message", e);
      }
   }

   private long scale(int size) {
      Address local = channel.address();
      int index = channel.view().getMembers().indexOf(local) + 1;
      if (size > 0 && index > size) {
         if (agent != null) {
            LOG.info("Scaling down, leaving the cluster: {}", channel.address());
            long start = System.nanoTime();
            agent.stop();
            agent = null;
            return System.nanoTime() - start;
         }
         return 0L;
      }

      if (agent != null) {
         LOG.info("Agent already existing, listening for state transfer");
         return 0L;
      }

      LOG.info("Joining the cluster now: {}", channel.address());
      boolean profiling = false;
      AsyncProfilerWrapper profiler = AsyncProfilerWrapper.create(channel.address().toString());
      if (configuration.isProfilingEnabled()) {
         profiler.start();
         profiling = true;
         LOG.info("Profiling scale-up operation");
      }

      long start = System.nanoTime();
      try {
         agent = factory.create();
      } catch (Throwable e) {
         LOG.error("Failed creating agent", e);
         return -1L;
      }

      agent.init();
      long completed = System.nanoTime() - start;
      if (profiling) {
         profiler.stop();
      }
      return completed;
   }

   private void scale(int size, Duration timeout) {
      scaleCollector.reset(channel.view().getMembers());

      AsyncProfilerWrapper profiler =  AsyncProfilerWrapper.create("coord-" + channel.address().toString());
      if (configuration.isProfilingEnabled()) {
         profiler.start();
      }
      CompletionStage<Long> local = agent.waitDataRehash();
      try {
         send(null, ProtocolStep.SCALE, size);
      } catch (Throwable t) {
         LOG.error("Failed to scale to {} members", size, t);
         return;
      }

      long v = 0;
      try {
         v = local.toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
         LOG.error("Never received rehash completed.", e);
      }

      boolean results = scaleCollector.waitForAllResponses(timeout.toMillis());
      if (!results)
         LOG.warn("Missing result from members: {}", scaleCollector.getMissing());

      if (configuration.isProfilingEnabled()) {
         profiler.stop();
      }

      scaleCollector.add(channel.address(), v);
      StringBuilder sb = new StringBuilder("Scaling cluster summary:").append(System.lineSeparator());
      for (Map.Entry<Address, Long> entry : scaleCollector.getResults().entrySet()) {
         sb.append(entry.getKey())
               .append(": ")
               .append(Util.printTime(entry.getValue(), TimeUnit.NANOSECONDS))
               .append(" (").append(TimeUnit.NANOSECONDS.toSeconds(entry.getValue())).append(" s)")
               .append(System.lineSeparator());
      }
      LOG.info(sb);
   }

   private void loadTest(ProtocolStep step, Duration timeout) {
      loadCollector.reset(channel.view().getMembers());

      try {
         send(null, step);
      } catch (Throwable t) {
         LOG.error("Failed to submit step {}", step, t);
         return;
      }

      boolean allResults = loadCollector.waitForAllResponses(timeout.toMillis());
      if (!allResults)
         LOG.warn("Missing results from members: {}", loadCollector.getMissing());

      if (step != ProtocolStep.EXECUTE)
         return;

      StringBuilder sb = new StringBuilder("Load test summary:").append(System.lineSeparator());

      long totalReqs = 0, totalTime = 0, longestTime = 0;
      long totalPuts = 0, totalGets = 0;
      for (Map.Entry<Address, Metric> entry : loadCollector.getResults().entrySet()) {
         Address mbr = entry.getKey();
         Metric result = entry.getValue();

         if (result != null) {
            totalReqs += result.puts() + result.gets();
            totalPuts += result.puts();
            totalGets += result.gets();
            totalTime += result.time();
            longestTime = Math.max(longestTime, result.time());
            sb.append(mbr).append(": ").append(result.summary()).append(System.lineSeparator());
         }
      }

      double reqSecNode = totalReqs / (totalTime / 1000f);
      double reqSecCluster = totalReqs / (longestTime / 1000f);
      String summary = String.format("Throughput: %,.0f reqs/sec/node %,.0f reqs/sec/cluster%nTotal: %d gets / %d puts",
            reqSecNode, reqSecCluster, totalGets, totalPuts);

      sb.append(System.lineSeparator()).append("\033[1m").append(summary).append("\033[0m").append(System.lineSeparator());
      LOG.info(sb);

      if (configuration.getOutputFile() != null) {
         LOG.info("Writing benchmark results to {}", configuration.getOutputFile());
         writeOutputToFile(loadCollector.getResults(), (int) longestTime);
      }
   }

   private void writeOutputToFile(Map<Address, Metric> result, int time) {
      Path dataDirectory = Path.of(configuration.getOutputFile());
      if (!Files.isDirectory(dataDirectory)) {
         LOG.warn("Output path {} is not a directory. Skipping file write", configuration.getOutputFile());
         return;
      }

      try (BufferedWriter reads = new BufferedWriter(new FileWriter(dataDirectory.resolve("reads.csv").toFile()));
           BufferedWriter writes = new BufferedWriter(new FileWriter(dataDirectory.resolve("writes.csv").toFile()))) {
         writes.write("Time");
         reads.write("Time");
         Set<Address> keys = result.keySet();
         for (Address address : keys) {
            writes.write(String.format(",%s", address));
            reads.write(String.format(",%s", address));
         }

         writes.newLine();
         reads.newLine();

         for (int i = 0; i < time; i++) {
            writes.write(Integer.toString(i));
            reads.write(Integer.toString(i));
            for (Address key : keys) {
               Metric metric = result.get(key);
               reads.write(String.format(",%d", metric.getsAt(i)));
               writes.write(String.format(",%d", metric.putsAt(i)));
            }
            writes.newLine();
            reads.newLine();
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void send(Address dest, ProtocolStep step, Object... args) throws Exception {
      if (args == null || args.length == 0) {
         channel.send(dest, new byte[]{(byte) step.ordinal()});
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
         case WARMUP -> {
            if (agent == null) {
               replyResult(msg.src(), Metric.empty());
               return;
            }
            agent.warmup().whenComplete((m, t) -> {
               if (t != null)
                  LOG.error("Failed executing warmup", t);
               replyResult(msg.src(), m);
            });
         }
         case EXECUTE -> {
            if (agent == null) {
               replyResult(msg.src(), Metric.empty());
               return;
            }
            agent.execute().whenComplete((m, t) -> {
               if (t != null)
                  LOG.error("Failed executing test", t);
               replyResult(msg.src(), m);
            });
         }
         case SCALE -> {
            ByteArrayDataInputStream in = new ByteArrayDataInputStream(buf, msg.getOffset() + 1, msg.getLength() - 1);
            int size = Util.objectFromStream(in);
            replyResult(msg.src(), scale(size));
         }
         case SCALE_REPLY -> {
            ByteArrayDataInputStream in = new ByteArrayDataInputStream(buf, msg.getOffset() + 1, msg.getLength() - 1);
            long duration = Util.objectFromStream(in);
            scaleCollector.add(msg.src(), duration);
         }
         case STOP -> agent.stop();
         case RESULTS -> {
            ByteArrayDataInputStream in = new ByteArrayDataInputStream(buf, msg.getOffset() + 1, msg.getLength() - 1);
            Metric metric = Util.objectFromStream(in);
            loadCollector.add(msg.src(), metric);
         }
         case SHUTDOWN -> {
            Util.close(channel);
            executor.close();
            completionListener.run();
         }
      }
   }

   private void replyResult(Address sender, long result) {
      try {
         send(sender, ProtocolStep.SCALE_REPLY, result);
      } catch (Exception e) {
         LOG.error("Failed to reply steps to {}", sender, e);
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
