package fiab.turntable.opcua.methods;

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
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.tracing.impl.zipkin.ZipkinUtil;
import fiab.turntable.actor.InternalTransportModuleRequest;

public class TransportRequest extends AbstractMethodInvocationHandler {

	final Duration timeout = Duration.ofSeconds(2);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ActorRef actor;

	public static final Argument RESPONSE = new Argument("start response", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText(
					"Response indicating (new) state of Transportmoduel whether transport request can be fulfilled."));

	public static final Argument CAP_FROM = new Argument("cap from", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("Capability Instance Id from which loading should happen"));

	public static final Argument CAP_TO = new Argument("cap to", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("Capability Instance Id to which unloading should happen"));

	public static final Argument ORDERID = new Argument("order id", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("not yet used"));

	public static final Argument REQID = new Argument("request id", Identifiers.String, ValueRanks.Scalar, null,
			new LocalizedText("not yet used"));

	public TransportRequest(UaMethodNode methodNode, ActorRef actor) {
		super(methodNode);
		this.actor = actor;
	}

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { CAP_FROM, CAP_TO, ORDERID, REQID };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[] { RESPONSE };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {

		logger.debug("Invoking TransportRequest() method of objectId={}", invocationContext.getObjectId());
		Object resp;
		try {
			String capIdFrom = (String) inputValues[0].getValue();
			String capIdTo = (String) inputValues[1].getValue();
			String orderId = (String) inputValues[2].getValue();
			String reqId = (String) inputValues[3].getValue();
			InternalTransportModuleRequest itmr = new InternalTransportModuleRequest(capIdFrom, capIdTo, orderId,
					reqId);

			Optional<B3Header> headerOpt = ModifiedSession.extractFromSession(invocationContext.getSession().get());
			if (headerOpt.isPresent()) {
				logger.info("Received B3 header: " + headerOpt.get().toString());
				B3Header b3 = headerOpt.get();
				itmr.setTracingHeader(ZipkinUtil.createB3Header(b3.spanId, b3.traceId, b3.parentId));
			} else {
				itmr.setTracingHeader("");
			}

			resp = ask(actor, itmr, timeout).toCompletableFuture().get();
			if (resp instanceof MachineStatusUpdateEvent) {
				resp = ((MachineStatusUpdateEvent) resp).getStatus();
			}
		} catch (InterruptedException | ExecutionException e) {
			logger.error(e.getMessage());
			resp = BasicMachineStates.UNKNOWN;
		}
		return new Variant[] { new Variant(resp.toString()) };
	}

}
