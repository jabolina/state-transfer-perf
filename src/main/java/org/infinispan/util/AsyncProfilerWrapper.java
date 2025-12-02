package org.infinispan.util;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import one.profiler.AsyncProfiler;

public class AsyncProfilerWrapper implements AutoCloseable {
   private static final Logger LOG = LogManager.getLogger(AsyncProfilerWrapper.class);

   private final String name;
   private final AsyncProfiler profiler;

   private AsyncProfilerWrapper(String name) {
      this.name = name;
      profiler = AsyncProfiler.getInstance();
   }

   public static AsyncProfilerWrapper create(String name) {
      return new AsyncProfilerWrapper(name);
   }

   public void start() {
      try {
         profiler.execute("start,event=cpu");
      } catch (IOException e) {
         LOG.error("Failed starting async-profiler", e);
      }
   }

   public void stop() {
      try {
         profiler.execute("stop,file=target/%p-" + name + ".html");
      } catch (IOException e) {
         LOG.error("Failed stopping async-profiler", e);
      }
   }

   @Override
   public void close() {
      stop();
   }
}
