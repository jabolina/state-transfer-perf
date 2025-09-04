package org.infinispan;

import java.time.Duration;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

public class Main {

    private static final Logger LOG = LogManager.getLogger(Main.class);

    @Command(name = "scaler", version = "0.0.1", mixinStandardHelpOptions = true)
    public static final class Scaler implements Callable<Integer> {

        @ArgGroup(validate = false, heading = "Control node configuration:%n", exclusive = false, headingKey = "control")
        ControlSection control;

        @ArgGroup(validate = false, heading = "Agent node configuration:%n", exclusive = false, headingKey = "agent")
        AgentSection agent;

        public static class ControlSection {
            @Option(names = {"-cc", "--control-configuration"}, required = true, description = "JGroups XML configuration file")
            protected String configuration;

            @Option(names = {"-cs", "--cluster-size"}, description = "Number of nodes in the cluster before starting the test. (${DEFAULT-VALUE})", defaultValue = "1")
            protected int clusterSize;

            @Option(names = {"-is", "--initial-size"}, description = "Initial cluster size. Coordinator is always created, negative value initialize all. (${DEFAULT-VALUE})", defaultValue = "-1")
            protected int initialSize;

            @Option(names = {"-ss", "--scale-size"}, description = "The number of nodes the cluster should scale to. Negative values do not change the cluster. (${DEFAULT-VALUE})", defaultValue = "-1")
            protected int scaleToSize;

            @Option(names = {"-f", "--first"}, description = "Mark this node as the controller")
            protected boolean controller;

            @Option(names = {"-o", "--output"}, description = "Location to write benchmark summary file", defaultValue = "null")
            protected String outputFile;

            public String getConfiguration() {
                return configuration;
            }

            public int getClusterSize() {
                return clusterSize;
            }

            public int getInitialSize() {
                return initialSize;
            }

            public int getScaleToSize() {
                return scaleToSize;
            }

            public boolean isController() {
                return controller;
            }

            public String getOutputFile() {
                return outputFile;
            }

            @Override
            public String toString() {
                return "ControlSection{" +
                        "configuration='" + configuration + '\'' +
                        ", clusterSize=" + clusterSize +
                        ", initialSize=" + initialSize +
                        ", scaleSize=" + scaleToSize +
                        ", controller=" + controller +
                        ", outputFile=" + outputFile +
                        '}';
            }
        }

        public static class AgentSection {
            @Option(names = {"-ac", "--agent-configuration"}, required = true, description = "Infinispan XML configuration file")
            protected String configuration;

            @Option(names = "--num-threads", description = "Number of worker threads submitting operations. (${DEFAULT-VALUE})", defaultValue = "100")
            protected int numThreads;

            @Option(names = "--num-keys", description = "Number of keys to insert and operate. (${DEFAULT-VALUE})", defaultValue = "100000")
            protected int keyspace;

            @Option(names = "--message-size", description = "The payload size to use as value. (${DEFAULT-VALUE})", defaultValue = "1000")
            protected int messageSize;

            @Option(names = "--warmup", description = "Warmup phase duration. (${DEFAULT-VALUE})", defaultValue = "PT1M")
            protected Duration warmupDuration;

            @Option(names = "--duration", description = "Test duration time. (${DEFAULT-VALUE})", defaultValue = "PT1M")
            protected Duration testDuration;

            @Option(names = "--read-ratio", description = "The read-write ratio. (${DEFAULT-VALUE})", defaultValue = "0.8")
            protected float readPercentage;

            public String getConfiguration() {
                return configuration;
            }

            public int getNumThreads() {
                return numThreads;
            }

            public int getKeyspace() {
                return keyspace;
            }

            public int getMessageSize() {
                return messageSize;
            }

            public Duration getWarmupDuration() {
                return warmupDuration;
            }

            public Duration getTestDuration() {
                return testDuration;
            }

            public float getReadPercentage() {
                return readPercentage;
            }

            @Override
            public String toString() {
                return "AgentSection{" +
                        "configuration='" + configuration + '\'' +
                        ", numThreads=" + numThreads +
                        ", keyspace=" + keyspace +
                        ", messageSize=" + messageSize +
                        ", warmupDuration=" + warmupDuration +
                        ", testDuration=" + testDuration +
                        ", readPercentage=" + readPercentage +
                        '}';
            }
        }

        @Override
        public Integer call() {
            LOG.info("Starting node with: {} -- {}", control, agent);

            Control c;
            try {
                c = Control.create(control, () -> Agent.create(agent));
            } catch (Exception e) {
                LOG.error("Failed creating control node", e);
                return 1;
            }

            try {
                c.start();
            } catch (Exception e) {
                LOG.info("Failed starting control node", e);
                return 2;
            }

            LOG.info("Waiting until test completion");
            c.bind();

            LOG.info("Stop control node");
            c.stop();
            return 0;
        }
    }

    public static void main(String[] args) throws Throwable {
        //String[] v = {"-cc", "configuration/local/control.xml", "-ac", "configuration/local/dist-sync.xml", "--first", "--warmup=PT10S", "--duration=PT20S", "--num-keys=20"};
        int exitCode = new CommandLine(new Scaler()).execute(args);
        System.exit(exitCode);
    }
}
