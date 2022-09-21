package fiab.mes.assembly.monitoring.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.mes.assembly.monitoring.actor.opcua.methods.NotifyPartPicked;
import fiab.mes.assembly.monitoring.message.PartsPickedNotification;
import fiab.mes.assembly.order.message.ExtendedRegisterProcessRequest;
import fiab.mes.eventbus.*;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.kie.api.logger.KieRuntimeLogger;
import org.kie.api.runtime.KieSession;

public class AssemblyMonitoringActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected ActorSelection orderEventBus;
    protected ActorSelection monitoringEventBus;
    protected OPCUABase server;
    protected KieSession kieSession;

    public static final String WELLKNOWN_LOOKUP_NAME = "AssemblyMonitoringActor";

    public static Props props(OPCUABase server, KieSession kieSession) {
        return Props.create(AssemblyMonitoringActor.class, () -> new AssemblyMonitoringActor(server, kieSession));
    }

    public AssemblyMonitoringActor(OPCUABase server, KieSession kieSession) {
        this.server = server;
        this.kieSession = kieSession;
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
                    kieSession.insert(notification);
                    kieSession.fireAllRules();
                })
                .match(ExtendedRegisterProcessRequest.class, req -> {
                    log.info(req.getRootOrderId());
                    log.info(req.getXmlRoot().toString());
                    OrderProcess orderProcess = new OrderProcess(req.getXmlRoot().getProcesses().get(0));
                    monitoringEventBus.tell(new RegisterProcessRequest(req.getRootOrderId(), orderProcess, self()), self());
                })
                .build();
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        kieSession.dispose();
    }

    protected void getEventBusAndSubscribe() {
        //Connect to eventbus for orderActor
        SubscribeMessage orderSub = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
        orderEventBus = this.context().actorSelection("/user/" + OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        orderEventBus.tell(orderSub, getSelf());
        //Connect to eventbus for planer
        SubscribeMessage planerEventBus = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(WELLKNOWN_LOOKUP_NAME, "*"));
        monitoringEventBus = this.context().actorSelection("/user/" + AssemblyMonitoringEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        monitoringEventBus.tell(planerEventBus, getSelf());
    }

    public void publishOnOrderEventBus(Object msg) {
        orderEventBus.tell(msg, self());
    }

    public void publishOnPlanerEventBus(Object msg) {
        monitoringEventBus.tell(msg, self());
    }
}
