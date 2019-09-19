package miloBasics.at.jku.isse.opc.milo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;

import com.google.common.collect.ImmutableList;

public class InternalClient implements Client{
	private boolean running = true;
	private boolean isConnected = false;
	private OpcUaClient theClient;

    @Override
    public String getEndpointUrl() {
    	return "opc.tcp://127.0.0.1:12686/javaMotorController";
    }
    
	@Override
	public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				running = false;
			}
		});
		try {
			client.connect().get();
			theClient = client;
			isConnected = true;
	        List<NodeId> nodeIds = ImmutableList.of(new NodeId(2, "HelloWorld/ScalarTypes/motor.currentSpeed"));
	        
	        CompletableFuture<DataValue> dataFuture = client.readValue(0.0, TimestampsToReturn.Both, nodeIds.get(0));
	        DataValue dValue = dataFuture.get();
	        System.out.println("currentSpeed :" + dValue.toString());
	        dataFuture.complete(dValue);
			while(running) {
				Thread.sleep(100);
			}
			System.out.println("ServerComm disconnected");
			isConnected = false;
			client.disconnect().get();
		} catch(Exception e) {
			System.out.println(e.getMessage());
			future.complete(client);
		}
		
	}
	public void shutdown() {
		System.out.println("Internal Client shutting down");
		running = false;
	}
	public void writeValues(List<NodeId> nodeIds, List<DataValue> dataValues) {
		
		/*try {
			while(!isConnected) Thread.sleep(100);
		} catch(Exception e) {
			
		}
		*/
		CompletableFuture<List<StatusCode>> status = theClient.writeValues(nodeIds, dataValues);
		status.complete(null);
	}
}
