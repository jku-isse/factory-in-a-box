package functionalUnits;

import communication.open62communication.ClientCommunication;

/**
 * Abstract base class for the process engine functional Unit
 * All process engine FUs should extend this class
 */
public abstract class ProcessEngineBase extends FunctionalUnitBase {

    private ClientCommunication clientCommunication;
    private Object client;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    private String serverUrl;

    public ClientCommunication getClientCommunication() {
        return clientCommunication;
    }

    public void setClientCommunication(ClientCommunication clientCommunication) {
        this.clientCommunication = clientCommunication;
    }

    public Object getClient() {
        return client;
    }

    public void setClient(Object client) {
        this.client = client;
    }

    /**
     * Loads a process.
     */
    public abstract void loadProcess(String info);

    /**
     * Resets the process engine
     */
    public abstract void reset();

    /**
     * Stops the process engine. Requires a reset in order to pass new process.
     */
    public abstract void stop();

    /**
     * Adds methods and variables to the server.
     * All nodes should be placed in the conveyor folder to enforce a clear structure.
    */
    public abstract void addServerConfig();
}
