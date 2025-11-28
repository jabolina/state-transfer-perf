package org.infinispan.agent;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.Agent;
import org.infinispan.Cache;
import org.infinispan.Main;
import org.infinispan.Metric;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.util.IntSets;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.IndexStorage;
import org.infinispan.data.HumongousEntry;
import org.infinispan.data.Person;
import org.infinispan.data.PersonKey;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.remoting.transport.Address;
import org.jgroups.util.Util;

final class TestAgent implements Agent {
   private static final Logger LOG = LogManager.getLogger(TestAgent.class);
   private static final String CACHE_NAME = "perf-cache";

   private final Main.Scaler.AgentSection configuration;
   private final EmbeddedCacheManager ecm;
   private final ExecutorService executor;

   private Cache<PersonKey, Object> cache;
   private volatile boolean initialized;

   TestAgent(Main.Scaler.AgentSection configuration, EmbeddedCacheManager ecm) {
      this.configuration = configuration;
      this.ecm = ecm;
      this.executor = Executors.newVirtualThreadPerTaskExecutor();
   }

   @Override
   public void init() {
      if (initialized) return;
      initialized = true;
      LOG.info("Connecting ECM");
      ecm.start();

      LOG.info("Define default cache for tests");

      try {
         ConfigurationBuilder builder = new ConfigurationBuilder();
         if (configuration.isIndexingEnabled()) {
            builder.indexing().enable()
                  .storage(IndexStorage.FILESYSTEM)
                  .addIndexedEntity(Person.class);
         }

         if (configuration.isPersistenceEnabled()) {
            builder.persistence().addSoftIndexFileStore();
         }

         builder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE);
         builder.clustering().cacheMode(CacheMode.DIST_SYNC);

         // Very large delay since we want to let it take as long as it needs.
         builder.clustering().stateTransfer().timeout(1, TimeUnit.DAYS);

         ecm.defineConfiguration(CACHE_NAME, builder.build());
         cache = ecm.getCache(CACHE_NAME);
      } catch (Exception e) {
         LOG.error("Failed creating cache", e);
         throw new RuntimeException("Failed creating cache", e);
      }
   }

   @Override
   public CompletionStage<Long> waitDataRehash() {
      if (cache == null) return CompletableFuture.completedFuture(0L);

      DataRehashListener listener = new DataRehashListener();
      cache.addListener(listener);
      return listener.join().whenComplete((ignore, t) -> cache.removeListener(listener));
   }

   @Override
   public void showOccupancy() {
      DistributionManager dm = ComponentRegistry.componentOf(cache, DistributionManager.class);
      ConsistentHash ch = dm.getCacheTopology().getCurrentCH();

      StringBuilder sb = new StringBuilder();
      StringBuilder summary = new StringBuilder();
      long clusterCount = 0;

      for (Address member : ch.getMembers()) {
         sb.append(member).append(":").append(System.lineSeparator());
         Set<Integer> segments = ch.getSegmentsForOwner(member);
         long totalEntries = 0;

         for (Integer segment : segments) {
            long count = cache.getAdvancedCache().keySet().stream()
                  .filterKeySegments(IntSets.from(Set.of(segment)))
                  .count();
            sb.append('\t').append(segment).append(": ").append(count).append(System.lineSeparator());
            totalEntries += count;
         }
         summary.append(member).append(": ").append(totalEntries).append(" (entries)").append(System.lineSeparator());
         sb.append('\t').append("Total: ")
               .append(segments.size()).append(" (segments) /")
               .append(totalEntries).append(" (entries)")
               .append(System.lineSeparator());
         clusterCount += totalEntries;
      }

      summary.append("Cluster count: ").append(clusterCount);
      //LOG.info(sb);
      LOG.info(summary);
   }

   @Override
   public void populate() {
      LOG.info("Populating cache with {} entries", configuration.getKeyspace());

      final int print = configuration.getKeyspace() / 10;
      AtomicInteger generate = new AtomicInteger(1);
      CountDownLatch latch = new CountDownLatch(configuration.getNumThreads());

      StringBuilder completion = new StringBuilder();
      CompletableFuture<?>[] cfs = new CompletableFuture[configuration.getNumThreads()];
      for (int i = 0; i < configuration.getNumThreads(); i++) {
         Object payload = configuration.isHumongousEnabled()
               ? HumongousEntry.createRandom()
               : Person.create();
         cfs[i] = CompletableFuture.runAsync(() -> {
            while (true) {
               final int key = generate.getAndIncrement();
               if (key > configuration.getKeyspace())
                  break;

               cache.put(new PersonKey(Integer.toString(key), "pseudonymas"), payload);
               if (print > 0 && key > 0 && key % print == 0) {
                  synchronized (completion) {
                     completion.append("=");
                     LOG.info("Populating: {}", completion.toString());
                  }
               }
            }
            latch.countDown();
         }, executor);
      }

      try {
         while (true) {
            int progress = generate.get();
            if (!latch.await(configuration.getTestDuration().toMillis(), TimeUnit.MILLISECONDS)) {
               if (progress != generate.get()) continue;
               LOG.warn("Took too long populating the cache");
               throw new IllegalStateException("Timed out to populate the cache");
            }
            break;
         }

         CompletableFuture.allOf(cfs).get(configuration.getWarmupDuration().multipliedBy(2).toMillis(), TimeUnit.MILLISECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
         LOG.error("Failed populating cache", e);
         throw new RuntimeException(e);
      }
   }

   @Override
   public CompletionStage<Metric> warmup() {
      return CompletableFuture.supplyAsync(() -> performLoad(configuration.getWarmupDuration()), executor);
   }

   @Override
   public CompletionStage<Metric> execute() {
      return CompletableFuture.supplyAsync(() -> performLoad(configuration.getTestDuration()), executor);
   }

   @Override
   public Main.Scaler.AgentSection configuration() {
      return configuration;
   }

   private Metric performLoad(Duration duration) {
      try {
         LOG.info("Performing load test for {}", duration);
         final CountDownLatch latch = new CountDownLatch(1);
         Invoker[] invokers = new Invoker[configuration.getNumThreads()];
         CompletableFuture<?>[] execution = new CompletableFuture[configuration.getNumThreads()];
         for (int i = 0; i < configuration.getNumThreads(); i++) {
            Object payload = configuration.isHumongousEnabled()
                  ? HumongousEntry.createRandom()
                  : Person.create();
            invokers[i] = new Invoker(latch, payload, duration);
            execution[i] = CompletableFuture.runAsync(invokers[i], executor);
         }

         latch.countDown();

         double tmp_interval = (duration.toSeconds() * 1000.0) / 10.0;
         long interval = (long) tmp_interval;
         Instant start = Instant.now();
         StringBuilder progress = new StringBuilder("=");
         while (duration.minus(Duration.between(start, Instant.now())).toMillis() >= 0) {
            Util.sleep(interval);
            LOG.info("Completion: {}", progress);
            progress.append("=");
         }

         Arrays.stream(invokers).forEach(Invoker::cancel);
         try {
            CompletableFuture.allOf(execution).join();
         } catch (Throwable t) {
            LOG.error("Exception when waiting invokers to finish", t);
         }

         long total = Duration.between(start, Instant.now()).toMillis();
         LOG.info("Load test finished in {} ms", total);

         Metric result = null;
         for (Invoker invoker : invokers) {
            if (result == null) {
               result = invoker.result();
               continue;
            }

            result = result.add(invoker.result());
         }

         if (result != null)
            LOG.info("Summary: {}", result.summary());
         else LOG.info("Result not found after completing load test");

         return result;
      } catch (Throwable t) {
         LOG.error("Failed to execute load test", t);
      }
      return null;
   }

   @Override
   public void stop() {
      ecm.stop();
      cache = null;
   }

   @Listener
   private static final class DataRehashListener {
      private final CompletableFuture<Long> cf;
      private volatile long start;

      private DataRehashListener() {
         this.cf = new CompletableFuture<>();
      }

      public CompletionStage<Long> join() {
         return cf;
      }

      @DataRehashed
      public void onDataRehash(DataRehashedEvent<Object, Object> ev) {
         if (!ev.isPre()) {
            cf.complete(System.nanoTime() - start);
         } else {
            start = System.nanoTime();
         }
      }
   }

   private final class Invoker implements Runnable {
      private final CountDownLatch latch;
      private final Object payload;
      private final Duration duration;
      private final long[] writeHistogram;
      private final long[] readHistogram;

      private volatile boolean running = true;
      private long reads;
      private long writes;

      private Invoker(CountDownLatch latch, Object payload, Duration duration) {
         this.latch = latch;
         this.payload = payload;
         this.duration = duration;

         int space = Math.toIntExact(duration.toSeconds());
         this.writeHistogram = new long[space];
         this.readHistogram = new long[space];
         Arrays.fill(writeHistogram, 0);
         Arrays.fill(readHistogram, 0);
      }

      public void cancel() {
         running = false;
      }

      @Override
      public void run() {
         try {
            latch.await();
         } catch (InterruptedException e) {
            LOG.error("Failed to wait other threads to start. Continuing...", e);
         }

         Instant testStart = Instant.now();
         while (Duration.between(testStart, Instant.now()).getSeconds() < duration.toSeconds()) {
            if (!running) break;

            int key = Util.random(configuration.getKeyspace());
            boolean isRead = Util.tossWeightedCoin(configuration.getReadPercentage());
            int index = (int) Duration.between(testStart, Instant.now()).toSeconds();

            try {
               if (isRead) {
                  Objects.requireNonNull(cache.get(key));
                  if (index < readHistogram.length)
                     readHistogram[index] += 1;
                  reads++;
               } else {
                  cache.put(new PersonKey(Integer.toString(key), "pseudonymas"), payload);
                  if (index < writeHistogram.length)
                     writeHistogram[index] += 1;
                  writes++;
               }
            } catch (Throwable t) {
               LOG.error("Failed performing cache operation", t);
            }
         }
      }

      public Metric result() {
         return new Metric(writes, writeHistogram, reads, readHistogram);
      }
   }
}
