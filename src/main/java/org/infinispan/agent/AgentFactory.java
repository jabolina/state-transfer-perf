package org.infinispan.agent;

import java.nio.file.Path;

import org.infinispan.Agent;
import org.infinispan.Main;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.data.TestGeneratedSchemaImpl;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

public final class AgentFactory {

    public static Agent create(Main.Scaler.AgentSection configuration) throws Throwable {
        long pid = ProcessHandle.current().pid();
        GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder().clusteredDefault();
        builder.serialization().addContextInitializer(new TestGeneratedSchemaImpl());
        builder.globalState().enabled(true)
                .persistentLocation(Path.of("target", Long.toString(pid)).toAbsolutePath().toString());
        EmbeddedCacheManager ecm = new DefaultCacheManager(builder.build(), false);
        return new TestAgent(configuration, ecm);
    }

    public interface Create {
        Agent create() throws Throwable;
    }
}
