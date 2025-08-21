package org.infinispan;

import java.util.concurrent.CompletionStage;

import org.infinispan.agent.AgentFactory;

public interface Agent {

    void init();

    void populate();

    CompletionStage<Metric> warmup();

    CompletionStage<Metric> execute();

    Main.Scaler.AgentSection configuration();

    void stop();

    static Agent create(Main.Scaler.AgentSection configuration) throws Throwable {
        return AgentFactory.create(configuration);
    }
}
