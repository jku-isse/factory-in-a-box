/**
 * @author micha
 * <p>
 * 6 Sep 2019
 */
package communication.open62communication;

import communication.utils.MonitoredItem;
import communication.utils.Pair;
import communication.utils.RequestedNodePair;
import open62Wrap.*;

import java.util.HashMap;


/**
 * @author micha
 */
public class ClientCommunication extends ClientAPIBase {

    private int i;
    private HashMap<Integer, MonitoredItem> monitoredItemSet;

    public ClientCommunication() {
        i = 0;
        monitoredItemSet = new HashMap<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopHandler(0)));
    }
/*
    public void addToMonitoredItems(RequestedNodePair<Integer, Integer> itemId, Object client) {
        UA_NodeId nodeId = open62541.UA_NODEID_NUMERIC(itemId.getKey(), itemId.getValue());
        int subscriptionId = clientSubToNode(this, client, nodeId);
        monitoredItemSet.put(getUaNodeNumeric(nodeId), new MonitoredItem(subscriptionId, clientReadIntValue(client, nodeId)));
    }

    public void removeFromMonitoredItems(RequestedNodePair<Integer, Integer> itemId, Object client) {
        UA_NodeId nodeId = open62541.UA_NODEID_NUMERIC(itemId.getKey(), itemId.getValue());
        int subscriptionId = monitoredItemSet.get(getUaNodeNumeric(nodeId)).getSubscriptionId();
        monitoredItemSet.remove(nodeId.getIdentifier().getNumeric());
        clientRemoveSub(client, subscriptionId);
    }

    public int getMonitoredItemValueById(RequestedNodePair<Integer, Integer> itemId) {
        UA_NodeId nodeId = open62541.UA_NODEID_NUMERIC(itemId.getKey(), itemId.getValue());
        if (monitoredItemSet.containsKey(getUaNodeNumeric(nodeId))) {
            return monitoredItemSet.get(getUaNodeNumeric(nodeId)).getValue();
        }
        System.out.println("Node with id not found!");
        return -1;
    }

    private int getUaNodeNumeric(UA_NodeId nodeId) {
        return nodeId.getIdentifier().getNumeric();
    }*/

    /**
     * Receiving interval calls from the c library when the client is connected
     * successfully to a server. This helps to set the monitored items at the
     * connected server after connection is successfully made. The interval is set
     * with this function call the c library UA_Client_run_iterate(client, 5000);
     */
    @Override
    public void client_connected(ClientAPIBase clientAPIBase, SWIGTYPE_p_UA_Client client, String serverUrl) {
        if (i < 1) {
            System.out.println("Client Connected " + i);
            i++;
        }
        /*
        System.out.println("Client Connected " + (i < 1 ? i : ""));
        if (i == 0) {
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

        /*System.out.println("============= monitored items changed ===============");
        if (monitoredItemSet.containsKey(getUaNodeNumeric(nodeId))) {
            System.out.println("Updated value " + getUaNodeNumeric(nodeId) + " to value: " + value);
            monitoredItemSet.get(getUaNodeNumeric(nodeId)).setValue(value);
        }
        System.out.println("Nothing to update");*/

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

    /*public int clientSubToNode(Object jClientAPIBase, Object client, RequestedNodePair<Integer, Integer> nodeID) {
        addToMonitoredItems(nodeID, client);
        return ClientAPIBase.ClientSubtoNode((ClientAPIBase) jClientAPIBase, (SWIGTYPE_p_UA_Client) client,
                open62541.UA_NODEID_NUMERIC(nodeID.getKey(), nodeID.getValue()));
    }*/

    public void clientRemoveSub(Object client, int subId) {
        ClientAPIBase.ClientRemoveSub((SWIGTYPE_p_UA_Client) client, subId);
    }

    /*
    public void clientRemoveSub(Object client, RequestedNodePair<Integer, Integer> nodeId, int subId) {
        removeFromMonitoredItems(nodeId, client);
        ClientAPIBase.ClientRemoveSub((SWIGTYPE_p_UA_Client) client, subId);
    }*/

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

    public int clientReadIntValueById(Object client, Pair<Integer, String> nodeId) {

        return ClientAPIBase.ClientReadIntValue((SWIGTYPE_p_UA_Client) client,
                ServerCommunication.CreateStringNodeId(nodeId.getKey(), nodeId.getValue()));
    }
    public int clientReadIntValueById(Object client, Object nodeId) {

        return ClientAPIBase.ClientReadIntValue((SWIGTYPE_p_UA_Client) client,(UA_NodeId) nodeId);
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
                ServerAPIBase.CreateStringNodeId(objectId.getKey(), objectId.getValue()),
                ServerAPIBase.CreateStringNodeId(methodId.getKey(), methodId.getValue()),
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

}