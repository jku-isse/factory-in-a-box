package communication.utils;

/**
 * This class stores the properties of MonitoredItems.
 */
public class MonitoredItem {

    private int subscriptionId;
    private int value;

    public MonitoredItem(int subscriptionId, int value) {
        this.subscriptionId = subscriptionId;
        this.value = value;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
