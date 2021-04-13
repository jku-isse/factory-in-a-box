package fiab.machine.plotter.opcua.methods;

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
import fiab.core.capabilities.plotting.PlotterMessageTypes;
import fiab.machine.plotter.messages.PlotterMessage;
import fiab.tracing.impl.zipkin.ZipkinUtil;

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
		PlotterMessage msg;
		if (headerOpt.isPresent()) {
			B3Header b3 = headerOpt.get();
			msg = new PlotterMessage(ZipkinUtil.createB3Header(b3.spanId, b3.traceId, b3.parentId),
					PlotterMessageTypes.Stop);
		} else
			msg = new PlotterMessage("", PlotterMessageTypes.Stop);
		actor.tell(msg, ActorRef.noSender());
		return new Variant[0];
	}

}
