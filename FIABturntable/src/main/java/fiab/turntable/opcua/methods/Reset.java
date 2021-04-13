package fiab.turntable.opcua.methods;

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
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.tracing.impl.zipkin.ZipkinUtil;
import fiab.turntable.actor.messages.TTModuleWellknwonCapabilityIdentifierMessage;

public class Reset extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ActorRef actor;

	public Reset(UaMethodNode methodNode, ActorRef actor) {
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
		logger.debug("Invoking Reset() method of objectId={}", invocationContext.getObjectId());

		Optional<B3Header> headerOpt = ModifiedSession.extractFromSession(invocationContext.getSession().get());
		TTModuleWellknwonCapabilityIdentifierMessage msg;
		if (headerOpt.isPresent()) {
			logger.info("Received B3 header: " + headerOpt.get().toString());
			B3Header b3 = headerOpt.get();
			msg = new TTModuleWellknwonCapabilityIdentifierMessage(ZipkinUtil.createB3Header(b3.spanId, b3.traceId, b3.parentId),
					TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset);
		} else {
			msg = new TTModuleWellknwonCapabilityIdentifierMessage("",
					TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset);
		}
		actor.tell(msg, ActorRef.noSender());
		return new Variant[0];
	}

}
