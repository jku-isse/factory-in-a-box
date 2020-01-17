package fiab.mes.machine.actor.iostation.wrapper;

import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockIOStationWrapper {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	//This wrapper will invoke the OPC-Server Methods. 
	//This can either be achieved by a client connected to the server, or internally in java
	
	private void callMethod(OpcUaClient client, NodeId objectId, NodeId methodId) {

        CallMethodRequest request = new CallMethodRequest(
            objectId, methodId, new Variant[]{new Variant(null)});
        
        client.call(request);
        logger.info("STOP Method has been called via CallMethodRequest");
    }
	//TODO
	public void callStop(OpcUaClient client) {
		callMethod(client, NodeId.parse("ns=2;s=OBJECT_NODE_HERE"), NodeId.parse("ns=2;s=METHOD_NODE_HERE"));
	}
	
	public void callReset(OpcUaClient client) {
		callMethod(client, NodeId.parse("ns=2;s=OBJECT_NODE_HERE"), NodeId.parse("ns=2;s=METHOD_NODE_HERE"));
	}
	
	
}
