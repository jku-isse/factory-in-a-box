package fiab.turntable.turning.statemachine;

/**
 * Triggers to advance through the conveyor state machine
 */
public enum TurningTriggers {
    RESET, RESETTING_DONE, START, EXECUTE, COMPLETE, COMPLETING_DONE, STOP, STOPPING_DONE
}
