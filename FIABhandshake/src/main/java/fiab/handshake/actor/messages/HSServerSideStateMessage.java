package fiab.handshake.actor.messages;

import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;

public class HSServerSideStateMessage extends AbstractHSExtensibleMessage<ServerSideStates> {
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
