package fiab.opcua.wiring;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

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

import main.java.fiab.core.capabilities.wiring.WiringInfo;

public class WiringRequest extends AbstractMethodInvocationHandler {
	
	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final WiringUpdateInterface wui;

	public WiringRequest(UaMethodNode node, WiringUpdateInterface wui) {
		super(node);
		this.wui = wui;
	}

	public static final Argument RESPONSE = new Argument("response", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText(
					"Response giving a staus update, if the wiring has worked or input parameters were wrong"));

	public static final Argument LOCAL_CAP_ID = new Argument("local capability id", Identifiers.String,
			ValueRanks.Scalar, null, new LocalizedText("Capability Instance Id from the local capability, required"));

	public static final Argument REMOTE_CAP_ID = new Argument("remote capability id", Identifiers.String,
			ValueRanks.Scalar, null, new LocalizedText("Capability Instance Id from the remote capability, required when setting"));
	public static final Argument REMOTE_ENDPOINT_URI = new Argument("remote endpoint uri", Identifiers.String,
			ValueRanks.Scalar, null, new LocalizedText("Uri of the server where the remote capability is, required when setting,  empty to unset"));
	public static final Argument REMOTE_NODE_ID = new Argument("remote node id", Identifiers.String, ValueRanks.Scalar,
			null, new LocalizedText("NodeId of the remote capability instance, required when setting"));
	public static final Argument REMOTE_ROLE = new Argument("remote role", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("Role not yet needed"));

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { LOCAL_CAP_ID, REMOTE_CAP_ID, REMOTE_ENDPOINT_URI, REMOTE_NODE_ID, REMOTE_ROLE };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[] { RESPONSE };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] params) throws UaException {
		String response = "Wiring completed";
		Variant var = new Variant(response);

		// check number of parameters
		if (params.length != 5) {
			response = "Wrong number of input parameters; 5 expected | " + params.length + " received";
			var = new Variant(response);
			return new Variant[] { var };
		}

		try {
			String localCapID = (String) params[0].getValue();
			String remoteCapabilityId = (String) params[1].getValue();
			String remoteEndpointURL = (String) params[2].getValue();
			String remoteNodeId = (String) params[3].getValue();
			String remoteRole = (String) params[4].getValue();
			WiringInfo wiringInfo;
			if (remoteEndpointURL != null && remoteEndpointURL.length() > 0) {
				URI uri = new URI(remoteEndpointURL);
				wiringInfo = new WiringInfo(localCapID, remoteCapabilityId, uri.toString(), remoteNodeId,
						remoteRole);
			} else {
				wiringInfo = new WiringInfo(localCapID, "", "", "", ""); 
				// used to unset wiring info
			}
			wui.provideWiringInfo(wiringInfo);

		} catch (URISyntaxException e) {

		} catch (Exception e) {
			e.printStackTrace();
			response = e.getLocalizedMessage();
			var = new Variant(response);
		}

		return new Variant[] { var };
	}

}
