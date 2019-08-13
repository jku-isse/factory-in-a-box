package functionalUnitBase;

import com.github.oxo42.stateless4j.StateMachine;
import open62Wrap.SWIGTYPE_p_UA_Server;
import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

import java.util.HashMap;
import java.util.function.Function;

public abstract class ProcessEngineBase {

    public abstract void loadProcess();

    public abstract void reset();   //TODO delete this if not necessary

    public abstract void stop();

    public abstract void addServerConfig(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId processFolder);
}
