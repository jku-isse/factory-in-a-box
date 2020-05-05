package fiab.turntable.turning;

public enum TurningTriggers {

    TURN_TO(0), EXECUTE(1), NEXT(2), RESET(3), STOP(4);

    int value;

    TurningTriggers(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
