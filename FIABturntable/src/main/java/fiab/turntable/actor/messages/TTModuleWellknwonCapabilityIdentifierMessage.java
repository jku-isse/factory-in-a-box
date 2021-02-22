package fiab.turntable.actor.messages;

import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.tracing.actor.messages.ExtensibleMessage;

public class TTModuleWellknwonCapabilityIdentifierMessage
		extends ExtensibleMessage<TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes> {
	private TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes body;

	public TTModuleWellknwonCapabilityIdentifierMessage(String header,
			TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes body) {
		super(header);
		this.body = body;
	}

	@Override
	public TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes getBody() {

		return body;
	}

}
