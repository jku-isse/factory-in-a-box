package fiab.handshake.fu.client.methods;

import org.eclipse.milo.opcua.sdk.server.ModifiedSession;
import org.eclipse.milo.opcua.sdk.server.ModifiedSession.B3Header;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.IOStationCapability;

import java.time.Duration;
import java.util.Optional;

public class Start extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private ActorRef actor;
	
    public Start(UaMethodNode methodNode, ActorRef actor) {
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
    	logger.debug("Invoking Start() method of objectId={}", invocationContext.getObjectId());    	
    	Optional<B3Header> headerOpt = ModifiedSession.extractFromSession(invocationContext.getSession().get());
    	if (headerOpt.isPresent()) {
    		// trace here, for now just a log output
    		logger.info("Received B3 header: "+headerOpt.get().toString());
    	}
    	
    	actor.tell(IOStationCapability.ClientMessageTypes.Start, ActorRef.noSender());
    	return new Variant[0]; 	    	
    }	
    
}
