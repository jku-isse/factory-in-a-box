package fiab.mes.machine.actor.iostation.wrapper;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.client.api.ServiceFaultListener;
//import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager.SubscriptionListener;
import org.eclipse.milo.opcua.sdk.client.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.IdType;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.opcua.CapabilityImplInfo;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class LocalIOStationActorSpawner extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    public static String nsPrefix = "2:";

    ActorRef machine;
    ActorRef discovery;

    public static Props props() {
        return Props.create(LocalIOStationActorSpawner.class, () -> new LocalIOStationActorSpawner());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CapabilityCentricActorSpawnerInterface.SpawnRequest.class, req -> {
                    log.info(String.format("Received CapabilityImplementationInfos for Cap: %s on %s", req.getInfo().getCapabilityURI(), req.getInfo().getEndpointUrl()));
                    discovery = getSender();
                    retrieveMethodAndVariableNodeIds(req);
                })
                .match(MachineDisconnectedEvent.class, req -> {
                    machine.tell(req, getSelf());
                    FiniteDuration duration = FiniteDuration.create(5, TimeUnit.SECONDS);
                    Future<Boolean> stopFuture = Patterns.gracefulStop(machine, duration);
                    Boolean stopped = Await.result(stopFuture, duration);
                    discovery.tell(req, getSelf());
                    getContext().stop(getSelf());
                })
                .build();
    }

    private void retrieveMethodAndVariableNodeIds(CapabilityCentricActorSpawnerInterface.SpawnRequest req) {
        // check if input or output station:
        String uri = req.getInfo().getCapabilityURI();
        boolean isInputStation = false;
        if (uri.equalsIgnoreCase(IOStationCapability.INPUTSTATION_CAPABILITY_URI))
            isInputStation = true;
        else if (uri.equalsIgnoreCase(IOStationCapability.OUTPUTSTATION_CAPABILITY_URI)) {
            isInputStation = false;
        } else { // something else, abort
            log.error("Called with nonsupported Capability: " + uri);
            return;
        }
        try {
            IOStationOPCUAnodes nodeIds = retrieveNodeIds(req.getInfo());
            if (!nodeIds.isComplete()) {
                log.error("Error obtaining methods and variables from OPCUA for spawning actor: " + nodeIds.toString());
                return;
            }
            Actor model = generateActor(req.getInfo());
            spawnNewIOStationActor(req.getInfo(), isInputStation, model, nodeIds.getStopMethod(), nodeIds.getResetMethod(), nodeIds.getStateVar());
//			req.getInfo().getClient().
            //});
        } catch (Exception e) {
            log.error("Error obtaining info from OPCUA for spawning IO actor with error: " + e.getMessage());
        }
    }

    private void spawnNewIOStationActor(CapabilityImplInfo info, boolean isInputStation, Actor model, NodeId stopMethod, NodeId resetMethod, NodeId stateVar) {
        final ActorSelection eventBusByRef = context().actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        AbstractCapability capability = isInputStation ? IOStationCapability.getInputStationCapability() : IOStationCapability.getOutputStationCapability();
        InterMachineEventBus intraEventBus = new InterMachineEventBus();
        IOStationOPCUAWrapper wrapper = new IOStationOPCUAWrapper(intraEventBus, info.getClient(), info.getActorNode(), stopMethod, resetMethod, stateVar, getSelf());
        machine = this.context().actorOf(BasicIOStationActor.props(eventBusByRef, capability, model, wrapper, intraEventBus), model.getActorName());

    }

    private Actor generateActor(CapabilityImplInfo info) throws InterruptedException, ExecutionException, UaException {
        UaNode actorNode = info.getClient().getAddressSpace().getNode(info.getActorNode());    //Maybe use async method instead?
        Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
        actor.setDisplayName(actorNode.getDisplayName().getText());
        actor.setActorName(actorNode.getBrowseName().getName());
        String id = info.getActorNode().getIdentifier().toString();
        String uri = info.getEndpointUrl().endsWith("/") ? info.getEndpointUrl() + id : info.getEndpointUrl() + "/" + id;
        actor.setID(uri);
        actor.setUri(uri);
        return actor;
    }

    private IOStationOPCUAnodes retrieveNodeIds(CapabilityImplInfo info) throws Exception {
        List<ReferenceDescription> referenceDescriptions = info.getClient().getAddressSpace().browse(info.getActorNode());
        // we assume unique node names and method names within this hierarchy level (thus no two capabilities with overlapping browse names)
        IOStationOPCUAnodes nodeIds = new IOStationOPCUAnodes();
        NamespaceTable namespaceTable = info.getClient().getNamespaceTable();
        for (ReferenceDescription referenceDescription : referenceDescriptions) {
            log.info("Checking node: " + referenceDescription.getBrowseName().toParseableString());
            String bName = referenceDescription.getBrowseName().getName();
            switch (Objects.requireNonNull(bName)) {
                case IOStationCapability.RESET_REQUEST:
                    nodeIds.setResetMethod(referenceDescription.getNodeId().toNodeIdOrThrow(namespaceTable));
                    break;
                case IOStationCapability.STOP_REQUEST:
                    nodeIds.setStopMethod(referenceDescription.getNodeId().toNodeIdOrThrow(namespaceTable));
                    break;
                case IOStationCapability.STATE_VAR_NAME:
                    nodeIds.setStateVar(referenceDescription.getNodeId().toNodeIdOrThrow(namespaceTable));
                    break;
            }
        }
        return nodeIds;
    }

    public static class IOStationOPCUAnodes {
        NodeId stopMethod;
        NodeId resetMethod;
        NodeId stateVar;

        public NodeId getStopMethod() {
            return stopMethod;
        }

        public void setStopMethod(NodeId stopMethod) {
            this.stopMethod = stopMethod;
        }

        public NodeId getResetMethod() {
            return resetMethod;
        }

        public void setResetMethod(NodeId resetMethod) {
            this.resetMethod = resetMethod;
        }

        public NodeId getStateVar() {
            return stateVar;
        }

        public void setStateVar(NodeId stateVar) {
            this.stateVar = stateVar;
        }

        public boolean isComplete() {
            return (stopMethod != null && resetMethod != null && stateVar != null);
        }

        @Override
        public String toString() {
            String stop = stopMethod != null ? stopMethod.toParseableString() : "NULL";
            String reset = resetMethod != null ? resetMethod.toParseableString() : "NULL";
            String state = stateVar != null ? stateVar.toParseableString() : "NULL";
            return "IOStationOPCUAnodes [stopMethod=" + stop + ", resetMethod=" + reset + ", stateVar="
                    + state + "]";
        }


    }

}
