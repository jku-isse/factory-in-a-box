package fiab.mes.transport.actor.bufferstation.msg;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;

public class UnloadRequest {
    protected String orderId;
    protected AkkaActorBackedCoreModelAbstractActor executor;

    public UnloadRequest(AkkaActorBackedCoreModelAbstractActor executor, String orderId) {
        this.executor = executor;
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public AkkaActorBackedCoreModelAbstractActor getExecutor() {
        return executor;
    }
}
