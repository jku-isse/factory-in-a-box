package fiab.turntable.turning.messages;

import fiab.core.capabilities.basicmachine.BasicMachineRequests;
import fiab.core.capabilities.functionalunit.FURequest;
import fiab.core.capabilities.transport.TransportDestinations;

public class TurnRequest extends FURequest {
	protected final TransportDestinations target;

	public TurnRequest(String senderId, TransportDestinations target) {
		super(senderId);
		this.target = target;
	}

	public TransportDestinations getTarget() {
		return target;
	}
	
}