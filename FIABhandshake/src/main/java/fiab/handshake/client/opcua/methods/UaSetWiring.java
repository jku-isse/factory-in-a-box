package fiab.handshake.client.opcua.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.client.messages.WiringRequest;
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

import java.net.URI;

public class UaSetWiring extends AbstractMethodInvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ActorRef actor;

    public static final Argument RESPONSE = new Argument("response",
            Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("Response giving a status update, if wiring has worked or input parameters were wrong"));

    public static final Argument LOCAL_CAP_ID = new Argument("local capability id",
            Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("Capability Instance Id from the local capability, required"));

    public static final Argument REMOTE_CAP_ID = new Argument("remote capability id",
            Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("Capability Instance Id from the remote capability, required when setting"));

    public static final Argument REMOTE_ENDPOINT_URI = new Argument("remote endpoint uri",
            Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("Uri of the server remote capability, required when setting, empty to unset"));

    public static final Argument REMOTE_NODE_ID = new Argument("remote node id",
            Identifiers.String, ValueRanks.Scalar,
            null, new LocalizedText("NodeId of the remote capability instance, required when setting"));

    public static final Argument REMOTE_ROLE = new Argument("remote role",
            Identifiers.String, ValueRanks.Scalar, null,
            new LocalizedText("Role not yet needed"));

    public UaSetWiring(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode);
        this.actor = actor;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{LOCAL_CAP_ID, REMOTE_CAP_ID, REMOTE_ENDPOINT_URI, REMOTE_NODE_ID, REMOTE_ROLE};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{RESPONSE};
    }

    @Override
    protected Variant[] invoke(AbstractMethodInvocationHandler.InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        logger.debug("Invoking Reset() method of objectId={}", invocationContext.getObjectId());
        String senderId = invocationContext.getObjectId().toParseableString();
        return collectWiringInfo(senderId, inputValues);
    }

    private Variant[] collectWiringInfo(String senderId, Variant[] inputValues) {
        String response = "Ok";
        // check number of parameters
        if (inputValues.length != 5) {
            response = "Wrong number of input parameters; 5 expected | " + inputValues.length + " received";
            return wrappedResponse(response);
        }
        try {
            applyWiringInfo(senderId, inputValues);
        } catch (Exception e) {
            e.printStackTrace();
            return wrappedResponse(e.getLocalizedMessage());
        }
        return wrappedResponse(response);
    }

    private void applyWiringInfo(String senderId, Variant[] inputValues) throws Exception {
        String localCapID = (String) inputValues[0].getValue();
        String remoteCapabilityId = (String) inputValues[1].getValue();
        String remoteEndpointURL = (String) inputValues[2].getValue();
        String remoteNodeId = (String) inputValues[3].getValue();
        String remoteRole = (String) inputValues[4].getValue();
        if (remoteEndpointURL != null && remoteEndpointURL.length() > 0) {
            URI uri = new URI(remoteEndpointURL);
            actor.tell(new WiringRequest(senderId, new WiringInfo(localCapID,
                    remoteCapabilityId, uri.toString(), remoteNodeId, remoteRole)), ActorRef.noSender());
        } else {
            // used to unset wiring info. Prevents us from setting incomplete wiringInfo
            actor.tell(new WiringRequest(senderId, new WiringInfo(localCapID,
                    "", "", "", "")), ActorRef.noSender());
        }
    }

    private Variant[] wrappedResponse(String response) {
        return new Variant[]{new Variant(response)};
    }
}
