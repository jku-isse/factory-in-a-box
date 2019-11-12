/**
 * @author micha
 * <p>
 * 6 Sep 2019
 */
package communication.open62communication;

import communication.utils.RequestedNodePair;
import helper.Pair;
import open62Wrap.*;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author micha
 */
public class ClientCommunication extends ClientAPIBase {

    private int i;
    private AtomicInteger conveyorStatus;
    private AtomicInteger turningStatus;
    private CopyOnWriteArrayList<UA_NodeId> monitoredItemSet;
    private int subscriptionConveyor;
    private int subscriptionTurning;

    public ClientCommunication() {
        i = 0;
        monitoredItemSet = new CopyOnWriteArrayList<>();
        conveyorStatus = new AtomicInteger(-1);
        turningStatus = new AtomicInteger(-1);
    }

    public int getConveyorStatus() {
        return conveyorStatus.get();
    }

    public int getTurningStatus() {
        return turningStatus.get();
    }

    public void addToMonitoredItems(RequestedNodePair<Integer, Integer> monitoredItem) {
        UA_NodeId nodeId = open62541.UA_NODEID_NUMERIC(monitoredItem.getKey(), monitoredItem.getValue());
        monitoredItemSet.add(nodeId);
    }

    /**
     * Receiving interval calls from the c library when the client is connected
     * successfully to a server. This helps to set the monitored items at the
     * connected server after connection is successfully made. The interval is set
     * with this function call the c library UA_Client_run_iterate(client, 5000);
     */
    @Override
    public void client_connected(ClientAPIBase clientAPIBase, SWIGTYPE_p_UA_Client client, String serverUrl) {
        System.out.println("Client Connected " + (i < 1 ? i : ""));
        /*
        System.out.println("Client Connected " + (i < 1 ? i : ""));
        if (i == 0) {
            RequestedNodePair<Integer, Integer> conveyorNode = new RequestedNodePair<>(1, 56);
            RequestedNodePair<Integer, Integer> turningNode = new RequestedNodePair<>(1, 57);
            subscriptionConveyor = clientSubToNode(clientAPIBase, client, open62541.UA_NODEID_NUMERIC(conveyorNode.getKey(), conveyorNode.getValue()));
            subscriptionTurning = clientSubToNode(clientAPIBase, client, open62541.UA_NODEID_NUMERIC(turningNode.getKey(), turningNode.getValue()));
            // if we subscribe to another node, the callback only uses the last node that was registered as the nodeId
            System.out.println("Client Connected");
            //System.out.println(getNodeByName(client, "Status")); // server by name
        }
        i++;

         */
    }

    /**
     * Receiving calls from the c library on monitored item(s) changed. can be
     * further filtered by Node id. As a first step considering the monitored item
     * value is int, but later could be changed to more generic variant.
     *
     * @param nodeId the node id triggered the change
     * @param value  the value of the node triggered the change
     */
    @Override
    public void monitored_itemChanged(UA_NodeId nodeId, int value) {
        /*
        //System.out.println("IMM FROM CLIENT Status monitored_itemChanged() invoked." + value);
        RequestedNodePair<Integer, Integer> conveyorNode = new RequestedNodePair<>(1, 56);  //bad practice and will be changed later
        RequestedNodePair<Integer, Integer> turningNode = new RequestedNodePair<>(1, 57);
        UA_NodeId conveyorId = open62541.UA_NODEID_NUMERIC(conveyorNode.getKey(), conveyorNode.getValue());
        UA_NodeId turningId = open62541.UA_NODEID_NUMERIC(turningNode.getKey(), turningNode.getValue());
        System.out.println("nodeId = " + nodeId.getIdentifier().getNumeric() + " | value = " + value);
        //System.out.println("nodeId = " + nodeId.getIdentifier().getNumeric() + " | expected = " + turningId.getIdentifier().getNumeric() + " | value = " + value);
        if(nodeId.getIdentifier().getNumeric() == conveyorId.getIdentifier().getNumeric()){
            System.out.println("Received callback for Conveyor");
            conveyorStatus.set(value);
        }else if(nodeId.getIdentifier().getNumeric() == turningId.getIdentifier().getNumeric()){
            System.out.println("Received callback for Turning");
            turningStatus.set(value);
        }

         */
    }

    public Object initClient() {
        return ClientAPIBase.InitClient();
    }

    public int clientConnect(Object jClientAPIBase, Object client, String serverUrl) {
        ClientAPIBase.ClientConnect((ClientAPIBase) jClientAPIBase, (SWIGTYPE_p_UA_Client) client, serverUrl);


        return 1;
    }

    public Object getNodeByName(Object client, String nodeName) {
        return ClientAPIBase.GetNodeByName((SWIGTYPE_p_UA_Client) client, nodeName);

    }

    public int clientSubToNode(Object jClientAPIBase, Object client, Object nodeID) {
        return ClientAPIBase.ClientSubtoNode((ClientAPIBase) jClientAPIBase, (SWIGTYPE_p_UA_Client) client,
                (UA_NodeId) nodeID);

    }

    public void clientRemoveSub(Object client, int subId) {
        ClientAPIBase.ClientRemoveSub((SWIGTYPE_p_UA_Client) client, subId);
    }

    public Object clientReadValue(Object client, Object nodeID) {
        return ClientAPIBase.ClientReadValue((SWIGTYPE_p_UA_Client) client, (UA_NodeId) nodeID);
    }

    public int clientReadIntValue(Object client, Object nodeID) {
        return ClientAPIBase.ClientReadIntValue((SWIGTYPE_p_UA_Client) client, (UA_NodeId) nodeID);
    }

    public int clientReadIntValueById(Object client, RequestedNodePair<Integer, Integer> nodeId) {
        return ClientAPIBase.ClientReadIntValue((SWIGTYPE_p_UA_Client) client,
                open62541.UA_NODEID_NUMERIC(nodeId.getKey(), nodeId.getValue()));
    }

    public int clientWriteValue(String serverUrl, Object nodeId, int value) {
        return ClientAPIBase.ClientWriteValue(serverUrl, (UA_NodeId) nodeId, value);
    }

    public String getMethodOutput() {
        return open62541JNI.ClientAPIBase_GetMethodOutput();
    }

    public String callMethod(String serverUrl, Object objectId, Object methodId, String argInputString) {
        return ClientAPIBase.CallMethod(serverUrl, (UA_NodeId) objectId, (UA_NodeId) methodId,
                argInputString);
    }
    public String callStringMethod(String serverUrl, Pair<Integer, String> objectId,
                                   Pair<Integer, String> methodId, String argInputString) {
        return ClientAPIBase.CallMethod(serverUrl,
                open62541.UA_NODEID_STRING(objectId.getKey(), objectId.getValue()),
                open62541.UA_NODEID_STRING(methodId.getKey(), methodId.getValue()),
                argInputString);
    }

    public String callStringMethod(String serverUrl, RequestedNodePair<Integer, Integer> objectId,
                                   RequestedNodePair<Integer, Integer> methodId, String argInputString) {
        return ClientAPIBase.CallMethod(serverUrl,
                open62541.UA_NODEID_NUMERIC(objectId.getKey(), objectId.getValue()),
                open62541.UA_NODEID_NUMERIC(methodId.getKey(), methodId.getValue()),
                argInputString);
    }

    public String callArrayMethod(String serverUrl, RequestedNodePair<Integer, Integer> objectId,
                                  RequestedNodePair<Integer, Integer> methodId, int argInput[]) {
        UA_Variant output = new UA_Variant();
        return ClientAPIBase.CallArrayMethod(serverUrl,
                open62541.UA_NODEID_NUMERIC(objectId.getKey(), objectId.getValue()),
                open62541.UA_NODEID_NUMERIC(methodId.getKey(), methodId.getValue()),
                argInput, argInput.length, output);
    }

    public Object createNodeString(int nameSpace,String id){
        return open62541.UA_NODEID_STRING(nameSpace, id);
    }
}