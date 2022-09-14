package fiab.mes.eventbus;

//import lombok.Value;

import fiab.functionalunit.connector.FUSubscriptionClassifier;

//Quick workaround to extend existing SubscriptionClassifier. Should consider having a common FIABSubClassifier
/*@Value*/ public class MESSubscriptionClassifier extends FUSubscriptionClassifier {

	protected String eventSource;
	protected  String topic;		

	 public MESSubscriptionClassifier(String eventSource, String topic) {
		super(eventSource, topic);
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
