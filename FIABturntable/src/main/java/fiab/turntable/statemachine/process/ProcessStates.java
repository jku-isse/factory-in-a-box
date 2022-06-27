package fiab.turntable.statemachine.process;

public enum ProcessStates {

    TURNING_SOURCE, HANDSHAKE_SOURCE, CONVEYING_SOURCE, TURNING_DEST, HANDSHAKE_DEST, CONVEYING_DEST,
    DONE, ABORTED, NOPROC
}
