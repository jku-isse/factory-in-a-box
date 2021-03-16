package fiab.mes.transport.msg;

import java.util.Optional;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.tracing.actor.messages.TracingHeader;

public class TransportModuleRequest implements TracingHeader {
	private String header;

	protected Position posFrom;
	protected Position posTo;
	protected String orderId;
	protected String requestId;
	protected TransportModuleRequest subsequentRequest = null;
	protected AkkaActorBackedCoreModelAbstractActor executor = null;
	
	public TransportModuleRequest(AkkaActorBackedCoreModelAbstractActor executor, Position posFrom, Position posTo,
			String orderId, String requestId) {
		this(executor, posFrom, posTo, orderId, requestId, null,"");
	}

	public TransportModuleRequest(AkkaActorBackedCoreModelAbstractActor executor, Position posFrom, Position posTo,
			String orderId, String requestId,String header) {
		this(executor, posFrom, posTo, orderId, requestId, null,header);
	}

	public TransportModuleRequest(AkkaActorBackedCoreModelAbstractActor executor, Position posFrom, Position posTo,
			String orderId, String requestId, TransportModuleRequest subsequentTMR,String header) {
		super();
		this.executor = executor;
		this.posFrom = posFrom == null ? TransportRoutingInterface.UNKNOWN_POSITION : posFrom;
		this.posTo = posTo == null ? TransportRoutingInterface.UNKNOWN_POSITION : posTo;
		this.orderId = orderId;
		this.requestId = requestId;
		this.subsequentRequest = subsequentTMR;
		this.header=header;
	}

	public String getOrderId() {
		return orderId;
	}

	public Position getPosFrom() {
		return posFrom;
	}

	public Position getPosTo() {
		return posTo;
	}

	public String getRequestId() {
		return requestId;
	}

	public Optional<TransportModuleRequest> getSubsequentRequest() {
		return Optional.ofNullable(subsequentRequest);
	}

	public AkkaActorBackedCoreModelAbstractActor getExecutor() {
		return executor;
	}

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	@Override
	public String getHeader() {
		return header;
	}
}
