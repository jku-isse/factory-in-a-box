package fiab.handshake.actor.messages;

import fiab.core.capabilities.handshake.HandshakeCapability.StateOverrideRequests;

public class HSStateOverrideRequestMessage extends AbstractHSExtensibleMessage<StateOverrideRequests> {

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
