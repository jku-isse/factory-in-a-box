package fiab.mes.transport.actor.bufferstation.wrapper;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.buffer.BufferStationCapability;
import fiab.core.capabilities.buffer.BufferStationWellKnownCapabilityIdentifiers;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.transport.actor.bufferstation.BasicBufferStationActor;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportmodule.wrapper.LocalTransportModuleActorSpawner;
import fiab.mes.transport.actor.transportmodule.wrapper.TransportModuleOPCUAWrapper;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.opcua.CapabilityImplInfo;
import fiab.turntable.actor.IntraMachineEventBus;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class LocalBufferStationActorSpawner extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected ActorRef machine;
    protected ActorRef discovery;

    public static Props props() {
        return Props.create(LocalBufferStationActorSpawner.class, () -> new LocalBufferStationActorSpawner());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CapabilityCentricActorSpawnerInterface.SpawnRequest.class, req -> {
                    log.info(String.format("Received CapabilityImplementationInfos for Cap: %s on %s", req.getInfo().getCapabilityURI(), req.getInfo().getEndpointUrl()));
                    discovery = getSender();
                    retrieveMethodAndVariableNodeIds(req);
                })
                .match(MachineConnectedEvent.class, req -> {
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
        if (!uri.equalsIgnoreCase(BufferStationWellKnownCapabilityIdentifiers.BUFFER_CAPABILITY_URI)) {
            log.error("Called with nonsupported Capability: " + uri);
            return;
        }
        try {
            LocalBufferStationActorSpawner.BufferStationOPCUAnodes nodeIds = retrieveNodeIds(req.getInfo());
            if (!nodeIds.isComplete()) {
                log.error("Error obtaining methods and variables from OPCUA for spawning actor: " + nodeIds.toString());
                return;
            }
            Actor model = generateActor(req.getInfo());
            spawnNewBufferStationActor(req.getInfo(), model, nodeIds);
        } catch (Exception e) {
            log.error("Error obtaining info from OPCUA for spawning actor with error: " + e.getMessage());
        }
    }

    private LocalBufferStationActorSpawner.BufferStationOPCUAnodes retrieveNodeIds(CapabilityImplInfo info) throws InterruptedException, ExecutionException {
        List<Node> nodes = info.getClient().getAddressSpace().browse(info.getActorNode()).get();
        // we assume unique node names and method names within this hierarchy level (thus no two capabilities with overlapping browse names)
        LocalBufferStationActorSpawner.BufferStationOPCUAnodes nodeIds = new LocalBufferStationActorSpawner.BufferStationOPCUAnodes();
        for (Node n : nodes) {
            log.info("Checking node: " + n.getBrowseName().get().toParseableString());
            String bName = n.getBrowseName().get().getName();
            if (bName.equalsIgnoreCase(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset.toString()))
                nodeIds.setResetMethod(n.getNodeId().get());
            else if (bName.equalsIgnoreCase(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Stop.toString()))
                nodeIds.setStopMethod(n.getNodeId().get());
            else if (bName.equalsIgnoreCase(BufferStationWellKnownCapabilityIdentifiers.OPCUA_LOAD_REQUEST))
                nodeIds.setLoadMethod(n.getNodeId().get());
            else if (bName.equalsIgnoreCase(BufferStationWellKnownCapabilityIdentifiers.OPCUA_UNLOAD_REQUEST))
                nodeIds.setUnloadMethod(n.getNodeId().get());
            else if (bName.equalsIgnoreCase(OPCUABasicMachineBrowsenames.STATE_VAR_NAME))
                nodeIds.setStateVar(n.getNodeId().get());
        }
        return nodeIds;
    }

    private void spawnNewBufferStationActor(CapabilityImplInfo info, Actor model, LocalBufferStationActorSpawner.BufferStationOPCUAnodes nodeIds) {
        final ActorSelection eventBusByRef = context().actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        AbstractCapability capability = BufferStationCapability.getBufferCapability();
        IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
        //TransportRoutingInterface.Position selfPos = resolvePosition(info);
        BufferStationOpcUaWrapper hal = new BufferStationOpcUaWrapper(intraEventBus, info.getClient(), info.getActorNode(), nodeIds.stopMethod, nodeIds.resetMethod, nodeIds.stateVar, nodeIds.loadMethod, nodeIds.unloadMethod, getSelf());
        HardcodedDefaultTransportRoutingAndMapping env = new HardcodedDefaultTransportRoutingAndMapping();
        machine = this.context().actorOf(BasicBufferStationActor.props(eventBusByRef, capability, model, intraEventBus, hal));
        log.info("Spawned Actor: " + machine.path());
    }

    private Actor generateActor(CapabilityImplInfo info) throws InterruptedException, ExecutionException {
        UaNode actorNode = info.getClient().getAddressSpace().getNodeInstance(info.getActorNode()).get();
        Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
        actor.setDisplayName(actorNode.getDisplayName().get().getText());
        actor.setActorName(actorNode.getBrowseName().get().getName());
        String id = info.getActorNode().getIdentifier().toString();
        String uri = info.getEndpointUrl().endsWith("/") ? info.getEndpointUrl() + id : info.getEndpointUrl() + "/" + id;
        //actor.setID(id);
        actor.setID(uri);
        actor.setUri(uri);
        return actor;
    }

    public static class BufferStationOPCUAnodes {
        NodeId stopMethod;
        NodeId resetMethod;
        NodeId stateVar;
        NodeId loadMethod;
        NodeId unloadMethod;

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

        public NodeId getLoadMethod() {
            return loadMethod;
        }

        public void setLoadMethod(NodeId loadMethod) {
            this.loadMethod = loadMethod;
        }

        public NodeId getUnloadMethod() {
            return unloadMethod;
        }

        public void setUnloadMethod(NodeId unloadMethod) {
            this.unloadMethod = unloadMethod;
        }

        public boolean isComplete() {
            return (stopMethod != null && resetMethod != null && stateVar != null && loadMethod != null && unloadMethod != null);
        }

        @Override
        public String toString() {
            String stop = stopMethod != null ? stopMethod.toParseableString() : "NULL";
            String reset = resetMethod != null ? resetMethod.toParseableString() : "NULL";
            String state = stateVar != null ? stateVar.toParseableString() : "NULL";
            String load = loadMethod != null ? loadMethod.toParseableString() : "NULL";
            String unload = unloadMethod != null ? unloadMethod.toParseableString() : "NULL";
            return "TransportModuleOPCUAnodes [stopMethod=" + stop + ", resetMethod=" + reset + ", stateVar="
                    + state + ", loadMethod=" + load + ", unloadMethod=" + unload + "]";
        }
    }

}
