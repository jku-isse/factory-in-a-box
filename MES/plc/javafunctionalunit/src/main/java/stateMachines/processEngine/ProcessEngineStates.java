package stateMachines.processEngine;

public enum ProcessEngineStates {

    IDLE(0), EXECUTING(1), RESETTING(2), STOPPED(3);

    private int value;

    ProcessEngineStates(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
