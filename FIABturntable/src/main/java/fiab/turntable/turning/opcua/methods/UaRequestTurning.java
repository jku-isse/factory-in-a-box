package fiab.turntable.turning.opcua.methods;

import java.time.Duration;
import java.util.Locale;

import fiab.core.capabilities.transport.TransportDestinations;
import fiab.turntable.turning.messages.TurnRequest;
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

public class UaRequestTurning extends AbstractMethodInvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ActorRef actor;

    public static final Argument TURN_POS = new Argument(
            "TurnToPos",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Position the Turntable should turn to: NORTH/EAST/SOUTH/WEST")
    );


    public UaRequestTurning(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode);
        this.actor = actor;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{TURN_POS};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[0];
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {

        logger.debug("Invoked TurnRequest() method of objectId={}", invocationContext.getObjectId());
        try {
            String direction = inputValues[0].getValue().toString().toUpperCase(Locale.ROOT);
            TransportDestinations target = TransportDestinations.valueOf(direction);
            // for now we ignore that we could have gotten a image id we don't support
            actor.tell(new TurnRequest(invocationContext.getObjectId().toParseableString(), target),
                    ActorRef.noSender());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return new Variant[0];
    }

}
