package turnTable;

/**
 * Triggers to advance through the state machine
 */
public enum TurnTableTriggers {
    RESET(0), LOAD(1), UNLOAD(2), NORTH(3), EAST(4), SOUTH(5), WEST(6), START(7), SEND_READY_TO_RECEIVE(8), SEND_LOADING_REQUEST(9), SEND_LOADING_START(10),
    SEND_UNLOADING_REQUEST(11), SEND_UNLOADING_START(12), NEXT(13), STOP(14);

    public final int id;

    TurnTableTriggers(int id) {
        this.id = id;
    }

}
