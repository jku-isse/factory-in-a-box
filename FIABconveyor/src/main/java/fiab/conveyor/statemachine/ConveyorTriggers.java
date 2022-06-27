package fiab.conveyor.statemachine;

/**
 * Triggers to advance through the conveyor state machine
 */
public enum ConveyorTriggers {
    RESET, RESET_DONE_EMPTY, RESET_DONE_FULL, LOAD, UNLOAD, LOADING_DONE, UNLOADING_DONE, STOP, STOP_DONE
}
