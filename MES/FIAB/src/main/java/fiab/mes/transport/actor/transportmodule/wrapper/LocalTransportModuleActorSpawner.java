package fiab.mes.transport.actor.transportmodule.wrapper;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//import org.eclipse.milo.opcua.sdk.client.api.AddressSpace;
//import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

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
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.opcua.CapabilityImplInfo;
import fiab.turntable.actor.IntraMachineEventBus;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import javax.lang.model.type.ReferenceType;


public class LocalTransportModuleActorSpawner extends AbstractActor {
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    ActorRef machine;
    ActorRef discovery;

    public static Props props() {
        return Props.create(LocalTransportModuleActorSpawner.class, () -> new LocalTransportModuleActorSpawner());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CapabilityCentricActorSpawnerInterface.SpawnRequest.class, req -> {
                    log.info(String.format("Received CapabilityImplementationInfos for Cap: %s on %s",req.getInfo().getCapabilityURI(), req.getInfo().getEndpointUrl()));
                    discovery = getSender();
                    retrieveMethodAndVariableNodeIds(req); })
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
        if (!uri.equalsIgnoreCase(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_CAPABILITY_URI))	{
            log.error("Called with nonsupported Capability: "+uri);
            return;
        }
        try {
            TransportModuleOPCUAnodes nodeIds = retrieveNodeIds(req.getInfo());
            if (!nodeIds.isComplete()) {
                log.error("Error obtaining methods and variables from OPCUA for spawning actor: "+nodeIds.toString());
                return;
            }
            log.info("Successfully retrieved node infos " + nodeIds);
            Actor model = generateActor(req.getInfo());
            log.info("Successfully Generated actor for " + req.getInfo());
            spawnNewIOStationActor(req.getInfo(), model, nodeIds);
        } catch(Exception e) {
            log.error("Error obtaining info from OPCUA for spawning transport actor with error: "+e.getMessage());
        }
    }

    private void spawnNewIOStationActor(CapabilityImplInfo info, Actor model, TransportModuleOPCUAnodes nodeIds) {
        final ActorSelection eventBusByRef = context().actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        AbstractCapability capability = TransportModuleCapability.getTransportCapability();
        IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
        Position selfPos = resolvePosition(info);
        TransportModuleOPCUAWrapper hal = new TransportModuleOPCUAWrapper(intraEventBus,  info.getClient(), info.getActorNode(), nodeIds.stopMethod, nodeIds.resetMethod, nodeIds.stateVar, nodeIds.transportMethod, getSelf());
        HardcodedDefaultTransportRoutingAndMapping env = new HardcodedDefaultTransportRoutingAndMapping();
        machine = this.context().actorOf(BasicTransportModuleActor.props(eventBusByRef, capability, model, hal, selfPos, intraEventBus, new TransportPositionLookup(), env), model.getActorName()+selfPos.getPos());
        log.info("Spawned Actor: "+machine.path());
    }

    private Position resolvePosition(CapabilityImplInfo info) {
        Position pos = TransportPositionLookup.parseLastIPPos(info.getEndpointUrl());
        if (pos == TransportRoutingInterface.UNKNOWN_POSITION || pos.getPos().equals("1")) {
            log.error("Unable to resolve position for uri via IP Addr, trying now via Port: "+info.getEndpointUrl());
            pos = TransportPositionLookup.parsePosViaPortNr(info.getEndpointUrl());
            if (pos == TransportRoutingInterface.UNKNOWN_POSITION) {
                log.error("Unable to resolve position for uri via port, assigning default position 20: "+info.getEndpointUrl());
                pos = new Position("20");
            }
        }
        return pos;
    }

    private Actor generateActor(CapabilityImplInfo info) throws UaException {
        log.info("Transport Actor info: Client=" + info.getClient() + ", ActorNode="+info.getActorNode() +
                ", Endpoint=" + info.getEndpointUrl() + ", CapURI=" + info.getCapabilityURI() + ", CapNode=" + info.getCapabilitiesNode());
        UaNode actorNode = info.getClient().getAddressSpace().getNode(info.getActorNode()); //Use async?
        log.info("Actor Node for creating actor: " + actorNode);
        Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
        actor.setDisplayName(actorNode.getDisplayName().getText());
        actor.setActorName(actorNode.getBrowseName().getName());
        String id = info.getActorNode().getIdentifier().toString();        
        String uri = info.getEndpointUrl().endsWith("/") ? info.getEndpointUrl()+id : info.getEndpointUrl()+"/"+id;
      //actor.setID(id);
        actor.setID(uri);
        actor.setUri(uri);
        return actor;
    }

    private TransportModuleOPCUAnodes retrieveNodeIds(CapabilityImplInfo info) throws Exception {
        List<ReferenceDescription> nodes = info.getClient().getAddressSpace().browse(info.getActorNode());
        // we assume unique node names and method names within this hierarchy level (thus no two capabilities with overlapping browse names)
        TransportModuleOPCUAnodes nodeIds = new TransportModuleOPCUAnodes();
        NamespaceTable namespaceTable = info.getClient().getNamespaceTable();
        for (ReferenceDescription n : nodes) {
            log.info("Checking transport module node: "+n.getBrowseName().toParseableString());
            String bName = n.getBrowseName().getName();
            log.info("Checking node with name " + bName);
            if(bName == null){
                continue;   //Just to be safe
            }
            if (bName.equalsIgnoreCase(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset.toString()))
                nodeIds.setResetMethod(n.getNodeId().toNodeIdOrThrow(namespaceTable));
            else if (bName.equalsIgnoreCase(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Stop.toString()))
                nodeIds.setStopMethod(n.getNodeId().toNodeIdOrThrow(namespaceTable));
            else if (bName.equalsIgnoreCase(TurntableModuleWellknownCapabilityIdentifiers.OPCUA_TRANSPORT_REQUEST))
                nodeIds.setTransportMethod(n.getNodeId().toNodeIdOrThrow(namespaceTable));
            else if (bName.equalsIgnoreCase(OPCUABasicMachineBrowsenames.STATE_VAR_NAME))
                nodeIds.setStateVar(n.getNodeId().toNodeIdOrThrow(namespaceTable));
        }
        log.info("NodeId retrieval for info " + info + " was successful");
        return nodeIds;
    }

    public static class TransportModuleOPCUAnodes {
        NodeId stopMethod;
        NodeId resetMethod;
        NodeId stateVar;
        NodeId transportMethod;

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
        public NodeId getTransportMethod() {
            return transportMethod;
        }
        public void setTransportMethod(NodeId transportMethod) {
            this.transportMethod = transportMethod;
        }
        public boolean isComplete() {
            return (stopMethod != null && resetMethod != null && stateVar != null && transportMethod != null);
        }
        @Override
        public String toString() {
            String stop = stopMethod != null ? stopMethod.toParseableString() : "NULL";
            String reset = resetMethod != null ? resetMethod.toParseableString() : "NULL";
            String state = stateVar != null ? stateVar.toParseableString() : "NULL";
            String transport = transportMethod != null ? transportMethod.toParseableString() : "NULL";
            return "TransportModuleOPCUAnodes [stopMethod=" + stop + ", resetMethod=" + reset + ", stateVar="
                    + state + ", transportMethod=" + transport + "]";
        }


    }
}
