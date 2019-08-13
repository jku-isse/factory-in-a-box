package functionalUnitDummys;

import functionalUnitBase.TurningBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import uaMethods.turningMethods.ResetTurningMethod;
import uaMethods.turningMethods.StopTurningMethod;
import uaMethods.turningMethods.TurnToMethod;
import turnTable.TurnTableOrientation;

import java.util.HashMap;
import java.util.function.Function;

public class TurningDummy extends TurningBase {
    @Override
    public void turnTo(TurnTableOrientation orientation) {
        System.out.println("Turn to was called with argument" + orientation.getNumericValue());
    }

    @Override
    public void reset() {
        System.out.println("Reset was called in Turning");
    }

    @Override
    public void stop() {
        System.out.println("Stop was called in Turning");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId turningFolder) {
        new TurnToMethod(this).addMethod(server, serverAPIBase, turningFolder);
        new ResetTurningMethod(this).addMethod(server, serverAPIBase, turningFolder);
        new StopTurningMethod(this).addMethod(server, serverAPIBase, turningFolder);
    }
}
