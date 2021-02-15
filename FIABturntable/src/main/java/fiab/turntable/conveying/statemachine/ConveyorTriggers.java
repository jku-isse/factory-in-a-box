package fiab.turntable.conveying.statemachine;

/**
 * Triggers to advance through the state machine
 */
public enum ConveyorTriggers {
    RESET(0), LOAD(1), UNLOAD(2), PAUSE(3),NEXT(4), STOP(5),
    NEXT_PARTIAL(6), NEXT_FULL(7);

    public final int value;

    ConveyorTriggers(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
