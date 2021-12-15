package fiab.mes.machine.msg;

import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;

import java.util.Objects;

public class OrderRelocationNotification extends MachineEvent {  //is machine event?

    private final String orderId;
    private final AkkaActorBackedCoreModelAbstractActor targetActor;

    public OrderRelocationNotification(String machineId, String orderId, AkkaActorBackedCoreModelAbstractActor targetActor){
        super(machineId, MachineEventType.UPDATED);
        this.orderId = orderId;
        this.targetActor = targetActor;
    }

    public String getOrderId() {
        return orderId;
    }

    public AkkaActorBackedCoreModelAbstractActor getTargetActor() {
        return targetActor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderRelocationNotification that = (OrderRelocationNotification) o;
        return Objects.equals(orderId, that.orderId) && Objects.equals(targetActor, that.targetActor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, targetActor);
    }

    @Override
    public String toString() {
        return "OrderRelocationNotification{" +
                "orderId='" + orderId + '\'' +
                ", targetActor=" + targetActor +
                '}';
    }
}
