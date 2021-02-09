package fiab.tracing.actor.messages;

public abstract class ExtensibleMessage<T> {
	private String header;

	public ExtensibleMessage(String header) {
		this.header = header;
	}

	public abstract T getBody();

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

}
