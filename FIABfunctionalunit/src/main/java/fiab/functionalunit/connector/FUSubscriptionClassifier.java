package fiab.functionalunit.connector;

public class FUSubscriptionClassifier {

    protected String eventSource;
    protected String topic;

    public FUSubscriptionClassifier(String eventSource, String topic) {
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
