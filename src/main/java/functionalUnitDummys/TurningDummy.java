package functionalUnitDummys;

import functionalUnitBase.TurningBase;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import turnTable.TurnTableOrientation;
import uaMethods.turningMethods.ResetTurningMethod;
import uaMethods.turningMethods.StopTurningMethod;
import uaMethods.turningMethods.TurnToMethod;
/**
 * Dummy class for the Turning FU. Used to test whether the server config is working properly
 */
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
