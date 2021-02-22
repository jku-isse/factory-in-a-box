package fiab.tracing.actor.messages;

public abstract class AbstractExtensibleMessage<T> implements ExtensibleMessage<T> {
	private String header;

	public AbstractExtensibleMessage() {
		this("");
	}

	public AbstractExtensibleMessage(String header) {
		super();
		this.header = header;
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public void setHeader(String header) {
		this.header = header;

	}

}
