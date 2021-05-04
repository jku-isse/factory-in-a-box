package fiab.tracing.actor.messages;

public class DummyMessage implements TracingHeader {
	private String header;
	
	public DummyMessage() {
		this("");
	}
	public DummyMessage(String header) {
		this.header =header;
	}
	

	@Override
	public void setTracingHeader(String header) {
		this.header =header;
	}

	@Override
	public String getTracingHeader() {
		return header;
	}

}
