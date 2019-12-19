package fiab.mes.transport.msg;

import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

public class TransportModuleRequest {

	protected Position posFrom;
	protected Position posTo;
	protected String orderId;
	
	
	public TransportModuleRequest(Position posFrom, Position posTo, String orderId) {
		super();
		this.posFrom = posFrom;
		this.posTo = posTo;
		this.orderId = orderId;
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
	
	
}
