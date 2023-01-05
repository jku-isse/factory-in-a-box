package fiab.handshake.server.opcua.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UaCompleteServerHandshake extends AbstractMethodInvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ActorRef actor;

    public UaCompleteServerHandshake(UaMethodNode methodNode, ActorRef actor) {
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
        logger.debug("Invoking Complete() method of objectId={}", invocationContext.getObjectId());
        actor.tell(new CompleteHandshake(invocationContext.getObjectId().toParseableString()), ActorRef.noSender());
        return new Variant[0];
    }

}