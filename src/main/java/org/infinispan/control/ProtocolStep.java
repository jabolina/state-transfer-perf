package org.infinispan.control;

enum ProtocolStep {
    WARMUP,
    EXECUTE,
    STOP,
    RESULTS,
    SHUTDOWN;

    public static ProtocolStep find(byte ordinal) {
        return values()[ordinal];
    }
}
