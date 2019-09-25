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
import communication.open62communication.ServerCommunication;
import communication.utils.RequestedNodePair;
import helper.CapabilityRole;
import helper.CapabilityType;
import helper.CapabilityId;

import java.util.function.Function;

public class Capability {
    private CapabilityId capabilityId;
    private CapabilityType capabilityType;
    private CapabilityRole capabilityRole;

    private ServerCommunication serverCommunication;
    private Object opcua_server;
    private Object parentObject;

    public Object getCapabilityObject() {
        return capabilityObject;
    }

    private Object capabilityObject;
    private Object capabilityOpcuaNodeId;

    public Capability(ServerCommunication serverCommunication, Object server, Object parentObject,
                      CapabilityId capabilityId, CapabilityType capabilityType , CapabilityRole capabilityRole) {
        this.capabilityId = capabilityId;
        this.capabilityType = capabilityType;
        this.capabilityRole = capabilityRole;

        this.serverCommunication = serverCommunication;
        this.opcua_server = server;
        this.parentObject = parentObject;
        capabilityOpcuaNodeId = serverCommunication.createNodeNumeric(1, serverCommunication.getUnique_id()); //need to implement a controller level Enum


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

}
