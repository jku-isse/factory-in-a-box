/**
 * [Class description.  The first sentence should be a meaningful summary of the class since it
 * will be displayed as the class summary on the Javadoc package page.]
 * <p>
 * [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
 * about desired improvements, etc.]
 *
 * @author Michael Bishara
 * @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
 * @author <A HREF="https://github.com/michaelanis14">[Github: Michael Bishara]</A>
 * @date 4 Sep 2019
 **/
package capabilities;

import communication.Communication;
import communication.open62communication.ClientCommunication;
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import helper.CapabilityRole;
import helper.CapabilityType;
import helper.CapabilityId;

import javax.swing.event.EventListenerList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.function.Function;

class CapabilityEvent extends EventObject {
    public CapabilityEvent(Capability source) {
        super(source);
    }
}
interface CapabilityListener extends EventListener {
    public void eventOccurred(CapabilityEvent evt);
}
public class Capability {
    private CapabilityId capabilityId;
    private CapabilityType capabilityType;
    private CapabilityRole capabilityRole;

    private ServerCommunication serverCommunication;

    public ClientCommunication getClientCommunication() {
        return clientCommunication;
    }

    public void setClientCommunication(ClientCommunication clientCommunication) {
        this.clientCommunication = clientCommunication;
    }

    private ClientCommunication clientCommunication;
    private Object opcua_server;
    private Object opcua_client;
    private Object parentObject;

    public Object getCapabilityObject() {
        return capabilityObject;
    }

    private Object capabilityObject;
    private Object capabilityOpcuaNodeId;
    public Capability(ClientCommunication clientCommunication, Object client, Object parentObject,
                      CapabilityId capabilityId, CapabilityType capabilityType , CapabilityRole capabilityRole) {
        this.capabilityId = capabilityId;
        this.capabilityType = capabilityType;
        this.capabilityRole = capabilityRole;

        this.clientCommunication = clientCommunication;
        this.opcua_client = client;
        this.parentObject = parentObject;
    }

    public Capability(ServerCommunication serverCommunication, Object server, Object parentObject,
                      CapabilityId capabilityId, CapabilityType capabilityType , CapabilityRole capabilityRole) {
        this.capabilityId = capabilityId;
        this.capabilityType = capabilityType;
        this.capabilityRole = capabilityRole;

        this.clientCommunication = new Communication().getClientCommunication(); // for testing only to be removed later

        this.serverCommunication = serverCommunication;
        this.opcua_server = server;
        this.parentObject = parentObject;
        capabilityOpcuaNodeId = serverCommunication.createNodeNumeric(1, serverCommunication.getUnique_id()); //TODO: need to implement a controller level Enum


        capabilityObject = serverCommunication.addNestedObject(opcua_server,parentObject, capabilityOpcuaNodeId, "CAPABILTY");

        Object id_nodeid = serverCommunication.addStringVariableNode(server, capabilityObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "ID");
        serverCommunication.writeVariable(server, id_nodeid, capabilityId.toString());

        Object type_nodeid = serverCommunication.addStringVariableNode(server, capabilityObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "TYPE");
        serverCommunication.writeVariable(server, type_nodeid, capabilityType.toString());

        Object role_nodeid = serverCommunication.addStringVariableNode(server, capabilityObject, new RequestedNodePair<>(1, serverCommunication.getUnique_id()), "ROLE");
        serverCommunication.writeVariable(server, role_nodeid, capabilityRole.toString());
    }

    public CapabilityId getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(CapabilityId capabilityId) {
        this.capabilityId = capabilityId;
    }

    public CapabilityType getCapabilityType() {
        return capabilityType;
    }

    public void setCapabilityType(CapabilityType capabilityType) {
        this.capabilityType = capabilityType;
    }


    public ServerCommunication getServerCommunication() {
        return serverCommunication;
    }

    public CapabilityRole getCapabilityRole() {
        return capabilityRole;
    }

    public void setCapabilityRole(CapabilityRole capabilityRole) {
        this.capabilityRole = capabilityRole;
    }

    /**
     * Returns the server. If server is not set it will return null
     *
     * @return server
     */
    public Object getServer() {
        return opcua_server;
    }

    public void setServer(Object server) {
        this.opcua_server = server;
    }

    public Object getObject() {
        return parentObject;
    }

    public void setObject(Object object) {
        this.parentObject = object;
    }

    protected Object addStringMethodToServer(RequestedNodePair<Integer, Integer> requestedNodePair, String methodName,
                                             Function<String, String> function) {
        return getServerCommunication().addStringMethod(getServerCommunication(), getServer(), getObject(),
                requestedNodePair, methodName, function);
    }

    protected EventListenerList listenerList = new EventListenerList();

    public void addMyEventListener(CapabilityListener listener) {
        listenerList.add(CapabilityListener.class, listener);
    }
    public void removeMyEventListener(CapabilityListener listener) {
        listenerList.remove(CapabilityListener.class, listener);
    }
    void fireMyEvent(CapabilityEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i = i+2) {
            if (listeners[i] == CapabilityListener.class) {
                ((CapabilityListener) listeners[i+1]).eventOccurred(evt);
            }
        }
    }
}
