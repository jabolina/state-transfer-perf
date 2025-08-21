package org.infinispan;

import org.jgroups.JChannel;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.control.ControlHandler;

public final class Control {
    private static final Logger LOG = LogManager.getLogger(Control.class);

    private final ControlHandler handler;
    private final CompletableFuture<Void> shutdown = new CompletableFuture<>();

    public Control(JChannel channel, Main.Scaler.ControlSection configuration, Agent agent) {
        this.handler = new ControlHandler(channel, agent, configuration, () -> shutdown.complete(null));
    }

    public static Control create(Main.Scaler.ControlSection configuration, Agent agent) throws Exception {
        JChannel ch = new JChannel(configuration.getConfiguration());
        return new Control(ch, configuration, agent);
    }

    public void start() throws Exception {
        handler.start();
    }

    public void bind() {
        shutdown.join();
    }

    public void stop() { }
}
