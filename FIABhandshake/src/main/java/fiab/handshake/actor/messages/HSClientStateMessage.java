package fiab.handshake.actor.messages;

import fiab.core.capabilities.handshake.HandshakeCapability.ClientSideStates;

public class HSClientStateMessage extends AbstractHSExtensibleMessage<ClientSideStates> {
	private final ClientSideStates body;

	public HSClientStateMessage(String header, ClientSideStates body) {
		super(header);
		this.body = body;
	}

	@Override
	public ClientSideStates getBody() {
		return body;
	}

}
