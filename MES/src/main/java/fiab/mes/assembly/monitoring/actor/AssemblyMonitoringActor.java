package fiab.mes.assembly.monitoring.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.mes.assembly.monitoring.actor.opcua.methods.NotifyPartPicked;
import fiab.mes.assembly.monitoring.message.PartsPickedNotification;
import fiab.mes.eventbus.*;
import fiab.mes.order.msg.OrderEvent;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;

public class AssemblyMonitoringActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected ActorSelection orderEventBus;
    protected ActorSelection monitoringEventBus;
    protected OPCUABase server;

    public static final String WELLKNOWN_LOOKUP_NAME = "AssemblyMonitoringActor";

    public static Props props(OPCUABase server) {
        return Props.create(AssemblyMonitoringActor.class, () -> new AssemblyMonitoringActor(server));
    }

    public AssemblyMonitoringActor(OPCUABase server) {
        this.server = server;
        getEventBusAndSubscribe();
        setupServerStructure();
    }

    private void setupServerStructure() {
        UaMethodNode partsPickedNode = server.createPartialMethodNode(server.getRootNode(), "NotifyPartsPicked",
                "Notify about parts picked");
        server.addMethodNode(server.getRootNode(), partsPickedNode, new NotifyPartPicked(partsPickedNode, self()));
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(OrderEvent.class, req -> monitoringEventBus.forward(req, context()))
                .match(PartsPickedNotification.class, notification -> {
                    log.info(notification.toString());
                })
                .build();
    }

    protected void getEventBusAndSubscribe() {
        //.getEventBusAndSubscribe();
        SubscribeMessage orderSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
        orderEventBus = this.context().actorSelection("/user/" + AssemblyMonitoringEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        orderEventBus.tell(orderSub, getSelf());

        SubscribeMessage machineSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
        monitoringEventBus = this.context().actorSelection("/user/" + OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        monitoringEventBus.tell(machineSub, getSelf());
    }
}
