package fiab.handshake.server.opcua.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;
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

public class UaStartServerHandshake extends AbstractMethodInvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Duration timeout = Duration.ofSeconds(2);
    private final ActorRef actor;

    public static final Argument RESPONSE = new Argument(
            "init response",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Response whether init request can be processed.")
    );

    public UaStartServerHandshake(UaMethodNode node, ActorRef actor) {
        super(node);
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
        HandshakeCapability.ServerMessageTypes resp;
        try {
            resp = ((ServerHandshakeResponseEvent) ask(actor,
                    new StartHandoverRequest(invocationContext.getObjectId().toParseableString()), timeout)
                    .toCompletableFuture().get()).getResponse();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage());
            resp = IOStationCapability.ServerMessageTypes.NotOkResponseStartHandover;
        }
        return new Variant[]{new Variant(resp.toString())};
    }
}
