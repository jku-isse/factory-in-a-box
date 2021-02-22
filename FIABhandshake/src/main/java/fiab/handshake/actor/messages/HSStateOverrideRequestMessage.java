package fiab.handshake.actor.messages;

import fiab.core.capabilities.handshake.HandshakeCapability.StateOverrideRequests;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class HSStateOverrideRequestMessage extends ExtensibleMessage<StateOverrideRequests> {

	private StateOverrideRequests body;

	public HSStateOverrideRequestMessage(String header, StateOverrideRequests body) {
		super(header);
		this.body = body;

	}

	@Override
	public StateOverrideRequests getBody() {
		return body;
	}

}
