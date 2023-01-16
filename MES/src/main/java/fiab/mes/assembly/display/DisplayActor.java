package fiab.mes.assembly.display;

import ExtensionsForAssemblyline.AssemblyHumanStep;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.mes.assembly.display.msg.ShowNextPossibleStepsRequest;
import fiab.mes.assembly.display.msg.ShowNextStepRequest;
import fiab.mes.eventbus.AssemblyMonitoringEventBusWrapperActor;
import fiab.mes.eventbus.DisplayEventBusWrapper;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.opcua.client.FiabOpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DisplayActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props(FiabOpcUaClient client, String id) {
        return Props.create(DisplayActor.class, () -> new DisplayActor(client, id));
    }

    private final String id;
    private final FiabOpcUaClient client;
    private final NodeId controlNodeId;
    private ActorSelection displayEventBus;

    public DisplayActor(FiabOpcUaClient client, String id) {
        this.client = client;
        this.controlNodeId = client.getNodeIdForBrowseName("control");
        this.id = id;
        subscribeToEventBus();
    }

    private void subscribeToEventBus(){
        SubscribeMessage subscribeMessage = new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(id, "*"));
        displayEventBus = this.context().actorSelection("/user/" + DisplayEventBusWrapper.WRAPPER_ACTOR_LOOKUP_NAME);
        displayEventBus.tell(subscribeMessage, getSelf());
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ShowNextPossibleStepsRequest.class, msg -> displayPossibleHumanAssemblySteps(msg.getSteps()))
                .match(ShowNextStepRequest.class, msg -> displayHumanAssemblyStep(msg.getStep()))
                .build();
    }

    protected void displayHumanAssemblyStep(AssemblyHumanStep step) {
        try {
            log.info("Calling displayNextStep on display...");
            client.callStringMethod(controlNodeId, new Variant("0"), new Variant(step.getName() + ": " + step.getDescription()))
                    .thenAccept(i -> log.info("Call on displayProxy succeeded for step: " + step));
        } catch (Exception e) {
            log.error("Call on displayNextStep failed!");
            e.printStackTrace();
        }

    }

    protected void displayPossibleHumanAssemblySteps(Collection<AssemblyHumanStep> steps) {
        //TODO there is no opcua call for this right now
    }
}
