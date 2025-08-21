package org.infinispan.agent;

import org.infinispan.Agent;
import org.infinispan.Main;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

public final class AgentFactory {

    public static Agent create(Main.Scaler.AgentSection configuration) throws Throwable {
        EmbeddedCacheManager ecm = new DefaultCacheManager(configuration.getConfiguration(), false);
        return new TestAgent(configuration, ecm);
    }
}
