package fiab.handshake.actor.messages;

import fiab.core.capabilities.handshake.HandshakeCapability.ClientMessageTypes;
import fiab.core.capabilities.handshake.IOStationCapability;


public class HSClientMessage extends AbstractHSExtensibleMessage<IOStationCapability.ClientMessageTypes> {
	private final ClientMessageTypes body;
	

	public HSClientMessage(String header, ClientMessageTypes body) {
		super(header);
		this.body = body;
	}

	@Override
	public ClientMessageTypes getBody() {

		return body;
	}

}
