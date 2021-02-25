package fiab.turntable.actor.messages;

import fiab.tracing.actor.messages.ExtensibleMessage;
import fiab.turntable.conveying.statemachine.ConveyorTriggers;

public class ConveyorTriggerMessage extends ExtensibleMessage<ConveyorTriggers> {

	private ConveyorTriggers body;

	public ConveyorTriggerMessage(String header, ConveyorTriggers body) {
		super(header);
		this.body = body;
	}

	@Override
	public ConveyorTriggers getBody() {
		return body;
	}

}
