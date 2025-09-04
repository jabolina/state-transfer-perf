package org.infinispan.agent;

import org.jgroups.util.Util;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.Agent;
import org.infinispan.Cache;
import org.infinispan.Main;
import org.infinispan.Metric;
import org.infinispan.manager.EmbeddedCacheManager;

final class TestAgent implements Agent {
    private static final Logger LOG = LogManager.getLogger(TestAgent.class);
    private static final String CACHE_NAME = "perf-cache";

    private final Main.Scaler.AgentSection configuration;
    private final EmbeddedCacheManager ecm;
    private final ExecutorService executor;

    private Cache<Integer, byte[]> cache;
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
        ecm.start();
        cache = ecm.getCache(CACHE_NAME);
    }

    @Override
    public void populate() {
        LOG.info("Populating cache with {} keys of size {} bytes", configuration.getKeyspace(), configuration.getMessageSize());

        final int print = configuration.getKeyspace() / 10;
        AtomicInteger generate = new AtomicInteger(1);
        byte[] payload = new byte[configuration.getMessageSize()];
        ThreadLocalRandom.current().nextBytes(payload);
        CountDownLatch latch = new CountDownLatch(configuration.getKeyspace());

        StringBuilder completion = new StringBuilder();
        for (int i = 0; i < configuration.getKeyspace(); i++) {
            executor.submit(() -> {
                final int key = generate.getAndIncrement();
                try {
                    cache.put(key, payload);
                    if (print > 0 && key > 0 && key % print == 0) {
                        synchronized (completion) {
                            completion.append("=");
                            LOG.info("Populating: {}", completion.toString());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            if (!latch.await(configuration.getWarmupDuration().toMillis(), TimeUnit.MILLISECONDS))
                throw new IllegalStateException("Timed out to populate the cache");
        } catch (InterruptedException e) {
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

            byte[] payload = new byte[configuration.getMessageSize()];
            ThreadLocalRandom.current().nextBytes(payload);
            final CountDownLatch latch = new CountDownLatch(1);
            Invoker[] invokers = new Invoker[configuration.getNumThreads()];
            CompletableFuture<?>[] execution = new CompletableFuture[configuration.getNumThreads()];
            for (int i = 0; i < configuration.getNumThreads(); i++) {
                invokers[i] = new Invoker(latch, payload, duration);
                execution[i] = CompletableFuture.runAsync(invokers[i], executor);
            }

            latch.countDown();

            double tmp_interval=(duration.toSeconds() * 1000.0) / 10.0;
            long interval=(long)tmp_interval;
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
    }

    private final class Invoker implements Runnable {
        private final CountDownLatch latch;
        private final byte[] payload;
        private final Duration duration;
        private final long[] writeHistogram;
        private final long[] readHistogram;

        private volatile boolean running = true;
        private long reads;
        private long writes;

        private Invoker(CountDownLatch latch, byte[] payload, Duration duration) {
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
                        cache.put(key, payload);
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
