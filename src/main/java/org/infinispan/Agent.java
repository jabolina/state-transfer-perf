package org.infinispan;

import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

public final class Agent {
    private final Main.Scaler.AgentSection configuration;
    private final EmbeddedCacheManager ecm;

    private Agent(Main.Scaler.AgentSection configuration, EmbeddedCacheManager ecm) {
        this.configuration = configuration;
        this.ecm = ecm;
    }

    public void start() {
        ecm.start();
    }

    public void stop() {
        ecm.stop();
    }

    public static Agent create(Main.Scaler.AgentSection configuration) throws Throwable {
        EmbeddedCacheManager ecm = new DefaultCacheManager(configuration.getConfiguration(), false);
        return new Agent(configuration, ecm);
    }
}
