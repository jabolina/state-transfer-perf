package org.infinispan.agent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.infinispan.Agent;
import org.infinispan.Main;
import org.infinispan.commons.configuration.io.ConfigurationResourceResolvers;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.data.TestGeneratedSchemaImpl;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

public final class AgentFactory {

    public static Agent create(Main.Scaler.AgentSection configuration) throws Throwable {
        long pid = ProcessHandle.current().pid();
       ConfigurationBuilderHolder holder = parseConfiguration("configuration/local/dist-sync.xml");
         //GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder().clusteredDefault();
        GlobalConfigurationBuilder builder = holder.getGlobalConfigurationBuilder();
        builder.serialization().addContextInitializer(new TestGeneratedSchemaImpl());
        builder.globalState().enabled(true)
                .persistentLocation(Path.of("target", Long.toString(pid)).toAbsolutePath().toString());
        builder.jmx().enabled(true)
              .domain("org.infinispan");
        EmbeddedCacheManager ecm = new DefaultCacheManager(holder, false);
//        EmbeddedCacheManager ecm = new DefaultCacheManager(builder.build(), false);
        return new TestAgent(configuration, ecm);
    }

   private static ConfigurationBuilderHolder parseConfiguration(String config) throws IOException {
      try (InputStream is = FileLookupFactory.newInstance().lookupFileStrict(config, Thread.currentThread().getContextClassLoader())) {
         return new ParserRegistry().parse(is, ConfigurationResourceResolvers.DEFAULT, MediaType.APPLICATION_XML);
      }
   }

    public interface Create {
        Agent create() throws Throwable;
    }
}
