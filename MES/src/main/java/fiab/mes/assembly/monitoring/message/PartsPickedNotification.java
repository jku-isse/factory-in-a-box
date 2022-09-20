package fiab.mes.assembly.monitoring.message;

public class PartsPickedNotification {

    private final String partId;
    private final String timeStamp;
    private final int amount;

    public PartsPickedNotification(String partId, String timeStamp, int amount) {
        this.partId = partId;
        this.timeStamp = timeStamp;
        this.amount = amount;
    }

    public String getPartId() {
        return partId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "PartsPickedNotification{" +
                "partId='" + partId + '\'' +
                ", timeStamp=" + timeStamp +
                ", amount=" + amount +
                '}';
    }
}
