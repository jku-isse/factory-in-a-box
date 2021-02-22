package fiab.handshake.actor.messages;

import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class HSServerSideStateMessage extends ExtensibleMessage<ServerSideStates> {
	private ServerSideStates body;

	public HSServerSideStateMessage(String header, ServerSideStates body) {
		super(header);
		this.body = body;
	}

	@Override
	public ServerSideStates getBody() {
		return body;
	}

}
