package fiab.mes.machine.actor.iostation.wrapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;


public class IOStationOPCUAWrapper implements IOStationWrapperInterface {

	private static final Logger logger = LoggerFactory.getLogger(IOStationOPCUAWrapper.class);
	
	private ActorRef intraMaschineBus;
	private OpcUaClient client;
	private NodeId capabilityImplNode;
	private NodeId stopMethod;
	private NodeId resetMethod;
	private NodeId stateVar;
	
	public IOStationOPCUAWrapper(ActorRef intraMaschineBus, OpcUaClient client, NodeId capabilityImplNode,
			NodeId stopMethod, NodeId resetMethod, NodeId stateVar) {
		super();
		this.intraMaschineBus = intraMaschineBus;
		this.client = client;
		this.capabilityImplNode = capabilityImplNode;
		this.stopMethod = stopMethod;
		this.resetMethod = resetMethod;
		this.stateVar = stateVar;
		logger.info("IOStationOPCUAWrapper initialized");
	}
	
//	private void init() throws InterruptedException, ExecutionException {
//		client.connect().get();
//	}

	private CompletableFuture<Boolean> callMethod(NodeId methodId) {

        CallMethodRequest request = new CallMethodRequest(
            capabilityImplNode, methodId, new Variant[]{});
         
        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                return CompletableFuture.completedFuture(Boolean.TRUE);
            } else {
                StatusCode[] inputArgumentResults = result.getInputArgumentResults();
                for (int i = 0; i < inputArgumentResults.length; i++) {
                    logger.error("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
                }
                
                CompletableFuture<Boolean> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }
	
	@Override
	public void stop() {
		callMethod(stopMethod);
		logger.info("Called STOP Method on OPCUA Node: "+stopMethod.toParseableString());
	}

	@Override
	public void reset() {
		callMethod(resetMethod).exceptionally(ex -> {
			logger.warn("Called RESET Method on OPCUA Node: "+resetMethod.toParseableString(), ex);
            return false;
        }).thenAccept(v -> {
        	if (v) 	logger.info("Called RESET Method successfully on OPCUA Node: "+resetMethod.toParseableString());
        });
		
	}

	@Override
	public void subscribeToStatus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void subscribeToLoadStatus() {
		// TODO Auto-generated method stub

	}

}
