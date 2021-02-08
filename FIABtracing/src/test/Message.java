package test;

import fiab.tracing.actor.messages.ExtensibleMessage;

public class Message extends ExtensibleMessage<Object> {

	public Message(String header) {
		super(header);
		
	}

	@Override
	public Object getBody() {
		// TODO Auto-generated method stub
		return null;
	}

}
