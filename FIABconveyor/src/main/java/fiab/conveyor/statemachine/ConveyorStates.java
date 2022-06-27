package fiab.conveyor.statemachine;

/**
 * States according to the conveyor state machine specification
 */
public enum ConveyorStates {
    IDLE_EMPTY, LOADING, UNLOADING, IDLE_FULL, RESETTING, STOPPING, STOPPED
}
