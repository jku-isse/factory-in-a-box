package fiab.handshake.server.opcua.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.server.TransportAreaStatusOverrideRequest;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UaSetLoaded extends AbstractMethodInvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    ActorRef actor;

    public UaSetLoaded(UaMethodNode node, ActorRef actor) {
        super(node);
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
        logger.debug("Invoking SetLoaded method of objectId={}", invocationContext.getObjectId());
        actor.tell(new TransportAreaStatusOverrideRequest(invocationContext.getObjectId().toParseableString(),
                HandshakeCapability.StateOverrideRequests.SetLoaded), ActorRef.noSender());
        return new Variant[0];
    }
}
