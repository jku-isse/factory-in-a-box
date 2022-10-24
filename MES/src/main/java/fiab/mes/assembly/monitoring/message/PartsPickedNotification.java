package fiab.mes.assembly.monitoring.message;

import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.core.capabilities.events.TimedEvent;

public class PartsPickedNotification extends TimedEvent {

    private final String partId;
    private final String timeStamp;
    private final int amount;

    public PartsPickedNotification(String partId, String timeStamp, int amount) {
        super();
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
