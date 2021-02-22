package fiab.handshake.actor.messages;

import fiab.core.capabilities.handshake.HandshakeCapability.ClientSideStates;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class HSClientStateMessage extends ExtensibleMessage<ClientSideStates> {
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
