package fiab.tracing.actor.messages;

public abstract class ExtensibleMessage<T> implements TracingHeader {
	private String header;

	public ExtensibleMessage(String header) {
		this.header = header;
	}

	public abstract T getBody();

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public String toString() {
		return getBody().toString();
	}

}
