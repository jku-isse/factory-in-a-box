package fiab.tracing.actor.messages;

public abstract class ExtensibleMessage<T> {
	private String header;

	public ExtensibleMessage(String header) {
		this.header = header;
	}

	public abstract T getBody();

}
