package fiab.machine.foldingstation.opcua.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineInWrongStateResponse;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.folding.FoldingMessageTypes;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static akka.pattern.Patterns.ask;

public class FoldRequest extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private ActorRef actor;
	
    public static final Argument RESPONSE = new Argument(
            "fold response",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Response indicating (new) state of FoldingStation whether fold request can be fulfilled.")
        );  
    
    public static final Argument IMAGE_ID = new Argument(
            WellknownFoldingCapability.FOLDING_CAPABILITY_INPUT_SHAPE_VAR_NAME,
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("ImageId that should be folded")
        );
    
    
    public FoldRequest(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode); 
        this.actor = actor;        
    }

    @Override
    public Argument[] getInputArguments() {    	
    	return new Argument[]{IMAGE_ID};    	    	
    }

    @Override
    public Argument[] getOutputArguments() {
    	return new Argument[]{RESPONSE};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
      
    	logger.debug("Invoked FoldRequest() method of objectId={}", invocationContext.getObjectId());
    	Object resp;
		try {
			String capIdFrom = (String) inputValues[0].getValue();
			// for now we ignore that we could have gotten a image id we don't support							
			resp = ask(actor, FoldingMessageTypes.Fold, timeout).toCompletableFuture().get();
			if (resp instanceof MachineStatusUpdateEvent) {
				 resp = ((MachineStatusUpdateEvent) resp).getStatus();
			}
			else if (resp instanceof MachineInWrongStateResponse) {
				resp = ((MachineInWrongStateResponse) resp).getStatus();
			}
		} catch (InterruptedException | ExecutionException e) {
			logger.error(e.getMessage());
			resp = BasicMachineStates.UNKNOWN;
		}    	        
        return new Variant[]{new Variant(resp.toString())};    	    	
    }	
    
}
