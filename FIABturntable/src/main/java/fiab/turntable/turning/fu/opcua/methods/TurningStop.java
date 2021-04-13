package fiab.turntable.turning.fu.opcua.methods;

import java.time.Duration;
import java.util.Optional;

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
import fiab.tracing.impl.zipkin.ZipkinUtil;
import fiab.turntable.actor.messages.TurningTriggerMessage;
import fiab.turntable.turning.statemachine.TurningTriggers;

public class TurningStop extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ActorRef actor;

	public TurningStop(UaMethodNode methodNode, ActorRef actor) {
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
		logger.debug("Invoking stop() method of objectId={}", invocationContext.getObjectId());
		Optional<B3Header> headerOpt = ModifiedSession.extractFromSession(invocationContext.getSession().get());
		TurningTriggerMessage msg;
		if (headerOpt.isPresent()) {
			B3Header b3 = headerOpt.get();
			msg = new TurningTriggerMessage(ZipkinUtil.createB3Header(b3.spanId, b3.traceId, b3.parentId),
					TurningTriggers.STOP);
		} else
			msg = new TurningTriggerMessage("", TurningTriggers.STOP);
		actor.tell(msg, ActorRef.noSender());
		return new Variant[0];
	}

}
