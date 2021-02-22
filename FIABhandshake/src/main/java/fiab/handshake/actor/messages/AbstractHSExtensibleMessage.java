package fiab.handshake.actor.messages;

import fiab.tracing.actor.messages.AbstractExtensibleMessage;

public abstract class AbstractHSExtensibleMessage<T> extends AbstractExtensibleMessage<T> {
	public AbstractHSExtensibleMessage(String header) {
		super(header);
	}

}
