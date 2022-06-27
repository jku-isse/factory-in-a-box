package fiab.conveyor.opcua.methods;

import java.time.Duration;

import fiab.core.capabilities.functionalunit.ResetRequest;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;

public class UaResetConveyor extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final ActorRef actor;
	
    public UaResetConveyor(UaMethodNode methodNode, ActorRef actor) {
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
    	logger.debug("Invoking reset() method of objectId={}", invocationContext.getObjectId());    	
    	actor.tell(new ResetRequest(invocationContext.getObjectId().toParseableString()), ActorRef.noSender());
    	return new Variant[0]; 	    	
    }	
    
}
