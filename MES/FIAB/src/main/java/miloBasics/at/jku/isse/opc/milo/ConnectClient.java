package miloBasics.at.jku.isse.opc.milo;

import java.lang.invoke.ConstantCallSite;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bouncycastle.asn1.x500.X500Name;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/*
 * This client 	connects to the SimpleMotor developped in C
 * 				controls the Motor speed using a PI-control scheme to compute motor torque
 * 				publishes the current speed of the motor to a new server (MyServer.java)
 * 				sports a modifiable target speed for the motor in MyServer
 * 				add and remove variable nodes via MyNamespace.java/STATIC_SCALAR_NODES
 */
public class ConnectClient implements Client{
	private JavaMotorController controllerConfig;
	private boolean running = true;
    public static void main(String[] args) throws Exception {
        ConnectClient example = new ConnectClient();

        new MyClientRunner(example).run();
    }
    private final Logger logger = LoggerFactory.getLogger(getClass());

    final double powerMax = 1.5;
    
    // set the url this client will connect to
    @Override
    public String getEndpointUrl() {
    	return "opc.tcp://localhost:4840";
    }
    
	@Override
	public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				running = false;
			}
		});
		try {
			// connect client to server
			client.connect().get();
			
			//startup 
			controllerConfig = new JavaMotorController();
			controllerConfig.startup().get();
	        
			List<NodeId> nodeIds = ImmutableList.of(new NodeId(1, "motor.speed"));
	        List<NodeId> writeIds = ImmutableList.of(new NodeId(1, "motor.torque"));
	        
	        long cStartTime = System.nanoTime() / 1000L;
	        long cDif = 0L;
	        long dt = 250000L; // time between control cycles, in us [microseconds] (1000 us = 1ms, 1000ms = 1s)
	        double intErr = 0.0; // storage variable for error integration over time
	        double oldTarget = 0.0;
	        
	        List<NodeId> updateIds = ImmutableList.of(new NodeId(2, "HelloWorld/ScalarTypes/motor.currentSpeed"));
	        java.util.Optional<org.eclipse.milo.opcua.sdk.server.nodes.ServerNode> nodeOpt = controllerConfig.getServer().getNodeMap().getNode(updateIds.get(0));
	        
	        List<NodeId> targetIds = ImmutableList.of(new NodeId(2, "HelloWorld/ScalarTypes/motor.targetSpeed"));
	        java.util.Optional<org.eclipse.milo.opcua.sdk.server.nodes.ServerNode> targetOpt = controllerConfig.getServer().getNodeMap().getNode(targetIds.get(0));
	        	        
	        double targetSpeed = 0.31415;
	        
	        while(running) {
	        	
	        	long cTime = System.nanoTime() / 1000L;
	        	if(cTime - cStartTime - cDif > dt) {

	    	        
	    	        CompletableFuture<DataValue> dataFuture = client.readValue(0.0, TimestampsToReturn.Both, nodeIds.get(0));
	    	        DataValue dValue = dataFuture.get();
	    	        dataFuture.complete(dValue);
	    	        double speed = (Double)dValue.getValue().getValue();
	    	        
	        		cDif += dt;

	    	        Variant varSpeed = new Variant(speed);
	    	        if(nodeOpt.isPresent()) {
	    	        	org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode varNode = ((org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode)nodeOpt.get());
	            		varNode.setValue(new DataValue(varSpeed));
	    	        }
	    	        if(targetOpt.isPresent()) {
	    	        	targetSpeed = (Double)(((org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode)targetOpt.get()).getValue().getValue().getValue());
	    	        }
	    	        
	        		double err = targetSpeed - speed;
	        		
	        		// fast error decay for 
	        		// dt = 0.25 seconds update interval
	        		// p = 0.7
	        		// i = 0.09
	        		
	        		
	        		
	        		double p = err * 0.70;
	        		intErr = Math.min(powerMax, Math.max(-powerMax, intErr + err*0.09));
	        		double i = intErr;
	        		System.out.println(String.format("p: %5.2f, i: %5.2f, u=p+i: %5.2f", p, i, p+i));
	    	        Variant varPower = new Variant(Math.min(Math.max(-powerMax, p+i),powerMax));
	    	        DataValue writing = new DataValue(varPower);
	    	        CompletableFuture<List<StatusCode>> writeFuture = client.writeValues(writeIds, ImmutableList.of(writing));
	    	        List<StatusCode> wValue = writeFuture.get();
	    	        StatusCode status = wValue.get(0);
	    	        if (!status.isGood()) {
	    	            logger.debug("Error on Write");
	    	        }
	    	        writeFuture.complete(wValue);
	        	}
	        	Thread.sleep(10);
	        }
			System.out.println("Terminating clean");
	        controllerConfig.shutdown().get();
			future.complete(client);
		} catch(Exception e) {
			System.out.println(e.getMessage());
			future.complete(client);
		}
	}
}
