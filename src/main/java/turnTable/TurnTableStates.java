package turnTable;

/**
 * The States according to the diagram
 */
public enum TurnTableStates {
    IDLE(0), STARTING(1), EXECUTING(2),
    //Substates of executing
    WAITING_FOR_UNLOADING_INITIATOR_REQUEST(3), WAITING_FOR_LOADING_INITIATOR_REPLY(4),
    WAITING_FOR_UNLOADING_INITIATOR_START(5), LOADING(6), TURNING_TO_DEST(7),
    WAITING_FOR_INITIATOR_UNLOADING_REPLY(8), UNLOADING(9),
    //States after execution
    COMPLETE(10), RESETTING(11);

    private int value;

    private TurnTableStates(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
