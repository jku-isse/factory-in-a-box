package fiab.mes.assembly.monitoring.actor.opcua.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.client.messages.WiringRequest;
import fiab.mes.assembly.monitoring.message.PartsPickedNotification;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class NotifyPartPicked extends AbstractMethodInvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ActorRef actor;

    public static final Argument RESPONSE = new Argument("response",
            Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("Part picking notification received"));

    public static final Argument PART_ID = new Argument("Part id",
            Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("Part id of picked part"));

    public static final Argument TIMESTAMP = new Argument("timestamp",
            Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("The part picking timestamp"));

    public static final Argument AMOUNT = new Argument("amount",
            Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("The amount of parts picked"));

    public NotifyPartPicked(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode);
        this.actor = actor;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{PART_ID, TIMESTAMP, AMOUNT};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{RESPONSE};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        logger.debug("Invoking NotifyPartPicked() method of objectId={}", invocationContext.getObjectId());
        String senderId = invocationContext.getObjectId().toParseableString();
        //return collectWiringInfo(senderId, inputValues);
        if (inputValues.length != 3) {
            return wrappedResponse("Invalid number of parameters");
        }
        String partId = inputValues[0].getValue().toString();
        String timeStamp = inputValues[1].getValue().toString();
        int amount = Integer.parseInt((String) inputValues[2].getValue());
        actor.tell(new PartsPickedNotification(partId, timeStamp, amount), ActorRef.noSender());
        return wrappedResponse("Ok");
    }

    private Variant[] wrappedResponse(String response) {
        return new Variant[]{new Variant(response)};
    }
}
