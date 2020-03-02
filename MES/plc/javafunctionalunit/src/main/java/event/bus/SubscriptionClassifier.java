package event.bus;

public class SubscriptionClassifier {

	protected String eventSource;
	protected  String topic;		

	public SubscriptionClassifier(String eventSource, String topic) {
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
