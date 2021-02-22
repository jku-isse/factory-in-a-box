package fiab.handshake.actor.messages;

import fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class HSServerMessage extends ExtensibleMessage<ServerMessageTypes> {
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
