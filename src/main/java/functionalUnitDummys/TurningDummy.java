package functionalUnitDummys;

import functionalUnitBase.TurningBase;
import functionalUnitDummys.turningMethods.ResetTurningMethod;
import functionalUnitDummys.turningMethods.StopTurningMethod;
import functionalUnitDummys.turningMethods.TurnToMethod;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import turnTable.TurnTableOrientation;

public class TurningDummy extends TurningBase {
    @Override
    public void turnTo(TurnTableOrientation orientation) {
        System.out.println("Turn to was called with argument" + orientation.getNumericValue());
    }

    @Override
    public void reset() {
        System.out.println("Reset was called");
    }

    @Override
    public void stop() {
        System.out.println("Stop was called");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase) {
        TurnToMethod.addMethod(server, serverAPIBase);
        ResetTurningMethod.addMethod(server, serverAPIBase);
        StopTurningMethod.addMethod(server, serverAPIBase);
    }
}
