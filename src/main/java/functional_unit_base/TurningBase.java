package functional_unit_base;

import turnTable.TurnTableOrientation;

public abstract class TurningBase {

    public abstract void turnTo(TurnTableOrientation orientation);

    public abstract void reset();

    public abstract void stop();
}
