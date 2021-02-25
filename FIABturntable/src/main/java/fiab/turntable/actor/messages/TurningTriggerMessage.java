package fiab.turntable.actor.messages;

import fiab.tracing.actor.messages.ExtensibleMessage;
import fiab.turntable.turning.statemachine.TurningTriggers;

public class TurningTriggerMessage extends ExtensibleMessage<TurningTriggers> {
	private TurningTriggers body;

	public TurningTriggerMessage(String header, TurningTriggers body) {
		super(header);
		this.body = body;
	}

	@Override
	public TurningTriggers getBody() {
		return body;
	}

}
