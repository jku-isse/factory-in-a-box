package fiab.mes.eventbus;

//import lombok.Value;

/*@Value*/ public class MESSubscriptionClassifier {

	protected String eventSource;
	protected  String topic;		

	 public MESSubscriptionClassifier(String eventSource, String topic) {
		super();
		this.eventSource = eventSource;
		this.topic = topic;
	}
	
	public String getEventSource() {
		return eventSource;
	}

	public String getTopic() {
		return topic;
	}
}
