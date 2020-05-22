package fiab.turntable.conveying;

/**
 * The States according to the diagram
 */
public enum ConveyorStates {
    IDLE(0), LOADING(3), UNLOADING(4), SUSPENDED(5), FULLY_OCCUPIED(6),
    RESETTING(7), STOPPING(8), STOPPED(9);

    private int value;

    ConveyorStates(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
