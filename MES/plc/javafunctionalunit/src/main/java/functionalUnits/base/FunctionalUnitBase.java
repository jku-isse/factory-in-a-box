package functionalUnits.base;

import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;

import java.util.function.Function;

public abstract class FunctionalUnitBase {

    private ServerCommunication serverCommunication;
    private Object server;
    private Object object;

    public ServerCommunication getServerCommunication() {
        return serverCommunication;
    }

    public void setServerCommunication(ServerCommunication serverCommunication) {
        this.serverCommunication = serverCommunication;
    }

    /**
     * Returns the server. If server is not set it will return null
     * @return server
     */
    public Object getServer() {
        return server;
    }
    /**
     * Sets the server to add methods and variables
     * @param server server to use
     */
    public void setServer(Object server) {
        this.server = server;
    }

    public Object getObject() {
        return object;
    }

    public void setFolder(Object folder) {
        this.object = folder;
    }

    public void setServerAndFolder(ServerCommunication serverCommunication, Object server, Object folder){
        this.serverCommunication = serverCommunication;
        this.server = server;
        this.object = folder;
    }

    protected Object addStringMethodToServer(RequestedNodePair<Integer, Integer> requestedNodePair, String methodName,
                                             Function<String, String> function) {
        return getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                requestedNodePair, methodName, function);
    }
}
