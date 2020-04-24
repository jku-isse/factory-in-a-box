package fiab.opcua.hardwaremock.iostation.methods;

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

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;

import static akka.pattern.Patterns.ask;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class InitHandover extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private ActorRef actor;
	
    public static final Argument RESPONSE = new Argument(
            "init response",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Response whether init request can be processed.")
        );    

    public InitHandover(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode); 
        this.actor = actor;        
    }

    @Override
    public Argument[] getInputArguments() {    	
    	return new Argument[0];    	    	
    }

    @Override
    public Argument[] getOutputArguments() {
    	return new Argument[]{RESPONSE};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        

    	logger.debug("Invoking InitHandover() method of objectId={}", invocationContext.getObjectId());    	
    	Object resp;
		try {
			resp = ask(actor, HandshakeProtocol.ServerMessageTypes.RequestInitiateHandover, timeout).toCompletableFuture().get();
		} catch (InterruptedException | ExecutionException e) {
			logger.error(e.getMessage());
			resp = HandshakeProtocol.ServerMessageTypes.NotOkResponseInitHandover;
		}    	        
        return new Variant[]{new Variant(resp.toString())};    	    	
    }

}
