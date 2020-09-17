package fiab.turntable.turning.statemachine;

public enum TurningStates {
    IDLE(0), STARTING(1), EXECUTING(2), COMPLETING(3), RESETTING(4), COMPLETE(5),
    STOPPING(6), STOPPED(7);

    private int value;

    TurningStates(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
