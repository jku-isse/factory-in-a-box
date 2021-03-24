package fiab.handshake.fu.server.methods;

import org.eclipse.milo.opcua.sdk.server.ModifiedSession;
import org.eclipse.milo.opcua.sdk.server.ModifiedSession.B3Header;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.handshake.actor.messages.HSServerMessage;

import java.time.Duration;
import java.util.Optional;

public class Stop extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ActorRef actor;

	public Stop(UaMethodNode methodNode, ActorRef actor) {
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
		logger.debug("Invoking Stop() method of objectId={}", invocationContext.getObjectId());
		Optional<B3Header> headerOpt = ModifiedSession.extractFromSession(invocationContext.getSession().get());
		if (headerOpt.isPresent()) {
			// TODO trace here, for now just a log output
			logger.info("Received B3 header: " + headerOpt.get().toString());
		}
		HSServerMessage msg = new HSServerMessage(headerOpt.get().spanId, IOStationCapability.ServerMessageTypes.Stop);

		actor.tell(msg, ActorRef.noSender());
		return new Variant[0];
	}

}
