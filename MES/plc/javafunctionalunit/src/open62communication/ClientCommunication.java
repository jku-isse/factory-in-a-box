/**
   [Class description.  The first sentence should be a meaningful summary of the class since it
   will be displayed as the class summary on the Javadoc package page.]
   
   [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
   about desired improvements, etc.]
   @author Michael Bishara
   @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
   @author <A HREF="https://github.com/michaelanis14">[Github]</A>
   @date 11 Sep 2019 
**/
package open62communication;

import open62Wrap.ClientAPIBase;
import open62Wrap.SWIGTYPE_p_UA_Client;
import open62Wrap.UA_NodeId;
import open62Wrap.open62541JNI;

/**
 * @author micha
 *
 */
public class ClientCommunication extends ClientAPIBase {

	int i = 0;

	public ClientCommunication() {

	}

	/**
	 * Receiving interval calls from the c library when the client is connected
	 * successfully to a server. This helps to set the monitored items at the
	 * connected server after connection is successfully made. The interval is set
	 * with this function call the c library UA_Client_run_iterate(client, 5000);
	 */
	@Override
	public void client_connected(ClientAPIBase clientAPIBase, SWIGTYPE_p_UA_Client client, String serverUrl) {
		System.out.println("Client Connected");

		if (i < 1) {
			i++;
			Object statusNodeID = getNodeByName(client, "Status"); // get Node id at the
			System.out.println(statusNodeID); // server by name
			int subId = clientSubtoNode(clientAPIBase, client, statusNodeID); //
			// subscribe to
			// changes at the
		}

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
		System.out.println("IMM FROM CLIENT Status monitored_itemChanged() invoked." + value);

	}

	public Object initClient() {
		return ClientAPIBase.InitClient();
	}

	public int clientConnect(Object jClientAPIBase, Object client, String serverUrl) {
		return ClientAPIBase.ClientConnect((ClientAPIBase) jClientAPIBase, (SWIGTYPE_p_UA_Client) client, serverUrl);

	}

	public Object getNodeByName(Object client, String nodeName) {
		return ClientAPIBase.GetNodeByName((SWIGTYPE_p_UA_Client) client, nodeName);

	}

	public int clientSubtoNode(Object jClientAPIBase, Object client, Object nodeID) {
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

	public int clientWriteValue(String serverUrl, Object nodeId, int value) {
		return ClientAPIBase.ClientWriteValue(serverUrl, (UA_NodeId) nodeId, value);
	}

	public String getMethodOutput() {
		return open62541JNI.ClientAPIBase_GetMethodOutput();
	}

	public String callMethod(Object client, Object objectId, Object methodId, String argInputString) {
		return ClientAPIBase.CallMethod((SWIGTYPE_p_UA_Client) client, (UA_NodeId) objectId, (UA_NodeId) methodId,
				argInputString);
	}

}
