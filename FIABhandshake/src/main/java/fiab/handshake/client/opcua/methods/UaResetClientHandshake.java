package fiab.handshake.client.opcua.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.handshake.IOStationCapability;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UaResetClientHandshake extends AbstractMethodInvocationHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final ActorRef actor;
	
    public UaResetClientHandshake(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode); 
        this.actor = actor;        
    }

    @Override
    public Argument[] getInputArguments() {    	
    	return new Argument[0];    	    	
    }

    @Override
    public Argument[] getOutputArguments() {
    	return new Argument[0];
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {        
    	logger.debug("Invoking Reset() method of objectId={}", invocationContext.getObjectId());    	
    	actor.tell(new ResetRequest(invocationContext.getObjectId().toParseableString()), ActorRef.noSender());
    	return new Variant[0]; 	    	
    }	
    
}
