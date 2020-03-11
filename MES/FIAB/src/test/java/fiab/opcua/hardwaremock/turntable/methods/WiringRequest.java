package fiab.opcua.hardwaremock.turntable.methods;

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

import akka.actor.ActorRef;
import fiab.opcua.hardwaremock.turntable.HandshakeFU;
import fiab.opcua.hardwaremock.turntable.WiringUtils.WiringInfo;

public class WiringRequest extends AbstractMethodInvocationHandler {
	private static final String PROTOCOL = "(https|http|opc.tcp)";
	private static final String NR = "(2[0-5][0-5]|[0-1]?[0-9]?[0-9])";
	private static final String IP_ADRESS = "(" + NR + "[\\.]){3}" + NR;
	private static final String PORT = "(6[0-5][0-5][0-3][0-5]|[0-5]?[0-9]{0,4})";
	private static final String END = "(/\\w+)*";
	private static final String IP_REGEX = PROTOCOL + "://" + "(" + IP_ADRESS + "|[a-zA-Z0-9]*):" + PORT + END;

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final HandshakeFU fu;

	public WiringRequest(UaMethodNode node, HandshakeFU fu) {
		super(node);
		this.fu = fu;
	}

	public static final Argument RESPONSE = new Argument("response", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText(
					"Response giving a staus update, if the wiring has worked or input parameters were wrong"));

	public static final Argument LOCAL_CAP_ID = new Argument("local capability id", Identifiers.String,
			ValueRanks.Scalar, null, new LocalizedText("Capability Instance Id from the local capability"));

	public static final Argument REMOTE_CAP_ID = new Argument("remote capability id", Identifiers.String,
			ValueRanks.Scalar, null, new LocalizedText("Capability Instance Id from the remote capability"));
	public static final Argument REMOTE_ENDPOINT_URI = new Argument("remote endpoint uri", Identifiers.String,
			ValueRanks.Scalar, null, new LocalizedText("Uri of the server where the remote capability is"));
	public static final Argument REMOTE_NODE_ID = new Argument("remote node id", Identifiers.String, ValueRanks.Scalar,
			null, new LocalizedText("NodeId of the remote capability instance"));
	public static final Argument REMOTE_ROLE = new Argument("remote role", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("Role ???"));

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
		String response = "Wired";
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

			if (!remoteEndpointURL.matches(IP_REGEX)) {
				response = "Wront Endpoint URL format: " + remoteEndpointURL;
				var = new Variant(response);
				return new Variant[] { var };
			}

			WiringInfo wiringInfo = new WiringInfo(localCapID, remoteCapabilityId, remoteEndpointURL, remoteNodeId,
					remoteRole);
			fu.provideWiringInfo(wiringInfo);

		} catch (Exception e) {
			e.printStackTrace();
			response = e.getLocalizedMessage();
			var = new Variant(response);
		}

		return new Variant[] { var };
	}

}
