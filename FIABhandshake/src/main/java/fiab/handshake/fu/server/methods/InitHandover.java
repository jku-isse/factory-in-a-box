package fiab.handshake.fu.server.methods;

import static akka.pattern.Patterns.ask;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.handshake.actor.messages.HSServerMessage;
import fiab.tracing.impl.zipkin.ZipkinUtil;

public class InitHandover extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ActorRef actor;

	public static final Argument RESPONSE = new Argument("init response", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("Response whether init request can be processed."));

	public InitHandover(UaMethodNode methodNode, ActorRef actor) {
		super(methodNode);
		this.actor = actor;
	}

	@Override
	public Argument[] getInputArguments() {
		return new Argument[0];
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[] { RESPONSE };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {

		logger.debug("Invoking InitHandover() method of objectId={}", invocationContext.getObjectId());
		Optional<B3Header> headerOpt = ModifiedSession.extractFromSession(invocationContext.getSession().get());
		Object resp;
		try {
			HSServerMessage msg;
			if (headerOpt.isPresent()) {
				logger.info("Received B3 header: " + headerOpt.get().toString());
				B3Header b3 = headerOpt.get();
				msg = new HSServerMessage(ZipkinUtil.createB3Header(b3.spanId, b3.traceId, b3.parentId),
						IOStationCapability.ServerMessageTypes.RequestInitiateHandover);
			} else {
				msg = new HSServerMessage("", IOStationCapability.ServerMessageTypes.RequestInitiateHandover);
			}
			resp = ask(actor, msg, timeout).toCompletableFuture().get();
		} catch (InterruptedException | ExecutionException e) {
			logger.error(e.getMessage());
			resp = IOStationCapability.ServerMessageTypes.NotOkResponseInitHandover;
		}
		return new Variant[] { new Variant(resp.toString()) };
	}

}
