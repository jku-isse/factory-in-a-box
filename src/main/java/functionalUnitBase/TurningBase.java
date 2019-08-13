package functionalUnitBase;

import com.github.oxo42.stateless4j.StateMachine;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;
import turnTable.TurnTableOrientation;

import java.util.HashMap;
import java.util.function.Function;

public abstract class TurningBase {

    public abstract void turnTo(TurnTableOrientation orientation);

    public abstract void reset();

    public abstract void stop();

    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId turningFolder);
}
