package org.infinispan;

import org.jgroups.JChannel;

public final class Control {
    private final JChannel channel;
    private final Main.Scaler.ControlSection configuration;

    public Control(JChannel channel, Main.Scaler.ControlSection configuration) {
        this.channel = channel;
        this.configuration = configuration;
    }

    public static Control create(Main.Scaler.ControlSection configuration) throws Exception {
        JChannel ch = new JChannel(configuration.getConfiguration());
        return new Control(ch, configuration);
    }

    public void start() throws Exception {
        channel.connect("scale-control");
    }

    public void stop() {
        channel.close();
    }
}
