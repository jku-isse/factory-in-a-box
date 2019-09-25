package stateMachines.processEngine;

public enum ProcessEngineStates {

    STOPPED(0), RESETTING(1), EXECUTING(2), IDLE(3);

    private int value;

    ProcessEngineStates(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
