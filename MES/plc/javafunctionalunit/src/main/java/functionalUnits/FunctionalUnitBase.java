package functionalUnits;

import communication.open62communication.ServerCommunication;

public abstract class FunctionalUnitBase {

    private ServerCommunication serverCommunication;
    private Object server;
    private Object Object;

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
        return Object;
    }

    public void setConveyorFolder(Object conveyorFolder) {
        this.Object = conveyorFolder;
    }

    public void setServerAndFolder(ServerCommunication serverCommunication, Object server, Object folder){
        this.serverCommunication = serverCommunication;
        this.server = server;
        this.Object = folder;
    }
}
