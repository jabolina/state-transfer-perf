package org.infinispan;

import org.jgroups.JChannel;

import java.util.concurrent.CompletableFuture;

import org.infinispan.agent.AgentFactory;
import org.infinispan.control.ControlHandler;

public final class Control {
    private final ControlHandler handler;
    private final CompletableFuture<Void> shutdown = new CompletableFuture<>();

    public Control(JChannel channel, Main.Scaler.ControlSection configuration, AgentFactory.Create factory) {
        this.handler = new ControlHandler(channel, factory, configuration, () -> shutdown.complete(null));
    }

    public static Control create(Main.Scaler.ControlSection configuration, AgentFactory.Create factory) throws Exception {
        JChannel ch = new JChannel(configuration.getConfiguration());
        return new Control(ch, configuration, factory);
    }

    public void start() throws Exception {
        handler.start();
    }

    public void bind() {
        shutdown.join();
    }

    public void stop() { }
}
