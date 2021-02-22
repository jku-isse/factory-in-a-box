package fiab.tracing.actor.messages;

public interface ExtensibleMessage<T> {

	public T getBody();

	public void setHeader(String header);

	public String getHeader();

}
