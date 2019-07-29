package turnTable;

/**
 * The States according to the diagram
 */
public enum TurnTableStates {
    IDLE, STARTING, EXECUTING,
    //Substates of executing
    WAITING_FOR_UNLOADING_INITIATOR_REQUEST, WAITING_FOR_LOADING_INITIATOR_REPLY,
    WAITING_FOR_UNLOADING_INITIATOR_START, LOADING, TURNING_TO_DEST,
    WAITING_FOR_INITIATOR_UNLOADING_REPLY, UNLOADING,
    //States after execution
    COMPLETE, RESETTING
}
