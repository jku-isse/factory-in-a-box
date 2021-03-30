package fiab.turntable.turning.fu.opcua.methods;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.ModifiedSession;
import org.eclipse.milo.opcua.sdk.server.ModifiedSession.B3Header;
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
import fiab.turntable.turning.TurnRequest;
import fiab.turntable.turning.TurnTableOrientation;

public class TurningRequest extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ActorRef actor;

	public static final Argument TURN_POS = new Argument("TurnToPos", Identifiers.Integer, ValueRanks.Scalar, null,
			new LocalizedText("Position the Turntable should turn to: on of 0,1,2,3"));

	public TurningRequest(UaMethodNode methodNode, ActorRef actor) {
		super(methodNode);
		this.actor = actor;
	}

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { TURN_POS };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[0];
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {

		logger.debug("Invoked TurnRequest() method of objectId={}", invocationContext.getObjectId());
		try {
			int pos = (Integer) inputValues[0].getValue();
			// for now we ignore that we could have gotten a image id we don't support
			Optional<B3Header> headerOpt = ModifiedSession.extractFromSession(invocationContext.getSession().get());
			TurnRequest req = new TurnRequest(TurnTableOrientation.createFromInt(pos));
			if(headerOpt.isPresent())
				req.setHeader(headerOpt.get().spanId);
			else
				req.setHeader("");
			actor.tell(req, ActorRef.noSender());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return new Variant[0];
	}

}
