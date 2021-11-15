package fiab.mes.machine.msg;

import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;

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

}
