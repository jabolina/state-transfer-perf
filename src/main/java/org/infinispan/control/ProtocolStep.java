package org.infinispan.control;

enum ProtocolStep {
    WARMUP,
    SCALE,
    SCALE_REPLY,
    EXECUTE,
    STOP,
    RESULTS,
    SHUTDOWN;

    public static ProtocolStep find(byte ordinal) {
        return values()[ordinal];
    }
}
