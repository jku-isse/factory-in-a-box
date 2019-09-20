package robot.turnTable;

/**
 * The directions where the turntable can look. Each has a numeric value and be constructed
 * from an integer. If this is not possible, it currently defaults to north.
 * We can also get the neighbors by using getNext(Counter)Clockwise()
 */
public enum TurnTableOrientation {
    NORTH(0), EAST(1), SOUTH(2), WEST(3);

    private int value;

    TurnTableOrientation(int value) {
        this.value = value;
    }

    public int getNumericValue() {
        return value;
    }

    public static TurnTableOrientation createFromInt(int i) {
        switch (i) {
            case 1:
                return EAST;
            case 2:
                return SOUTH;
            case 3:
                return WEST;
            default:
                return NORTH;
        }
    }

    public TurnTableOrientation getNextClockwise(TurnTableOrientation current) {
        switch (current) {
            case NORTH:
                return EAST;
            case EAST:
                return SOUTH;
            case SOUTH:
                return WEST;
            default:
                return NORTH;
        }
    }

    public TurnTableOrientation getNextCounterClockwise(TurnTableOrientation current) {
        switch (current) {
            case NORTH:
                return WEST;
            case SOUTH:
                return EAST;
            case WEST:
                return SOUTH;
            default:
                return NORTH;
        }
    }
}
