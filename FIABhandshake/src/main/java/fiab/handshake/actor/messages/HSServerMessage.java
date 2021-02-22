package fiab.handshake.actor.messages;

import fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes;

public class HSServerMessage extends AbstractHSExtensibleMessage<ServerMessageTypes> {
	private final ServerMessageTypes body;

	public HSServerMessage(String header,ServerMessageTypes body) {
		super(header);
		this.body = body;
	}

	@Override
	public ServerMessageTypes getBody() {
		return body;
	}

}
