package fiab.mes.machine.actor.foldingstation.wrapper;

import ActorCoreModel.Actor;
import ActorCoreModel.ActorCoreModelFactory;
import ProcessCore.AbstractCapability;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.machine.foldingstation.IntraMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.foldingstation.FoldingStationActor;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.productioncell.FoldingProductionCell;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.opcua.CapabilityImplInfo;
//import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class LocalFoldingStationActorSpawner extends AbstractActor {

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    ActorRef machine;
    ActorRef discovery;
    ActorRef outputStation;
    private final TransportPositionParser transportPositionParser;

    public static Props props(TransportPositionParser parser) {
        return Props.create(LocalFoldingStationActorSpawner.class, () -> new LocalFoldingStationActorSpawner(parser));
    }

    public LocalFoldingStationActorSpawner(TransportPositionParser transportPositionParser) {
        if (transportPositionParser == null) {
            this.transportPositionParser = new DefaultTransportPositionLookup();
        } else {
            this.transportPositionParser = transportPositionParser;
        }
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
        if (!uri.startsWith(WellknownFoldingCapability.FOLDING_CAPABILITY_BASE_URI)) {
            log.error("Called with nonsupported Capability: " + uri);
            return;
        }
        try {
            LocalFoldingStationActorSpawner.FoldingOPCUAnodes nodeIds = retrieveNodeIds(req.getInfo());
            if (!nodeIds.isComplete()) {
                log.error("Error obtaining methods and variables from OPCUA for spawning actor, shutting down: " + nodeIds);
                getSelf().tell(Kill.getInstance(), self());
                return;
            }
            Actor model = generateActor(req.getInfo());
            Actor outputModel = generateOutputActor(req.getInfo(), nodeIds);
            //if (outputModel == null) {   //If there is no outputstation connected via the wiringInfo, spawn without out
            //    spawnNewActor(req.getInfo(), model, nodeIds);
            //} else {
            spawnNewActor(req.getInfo(), model, outputModel, nodeIds);
            //}
        } catch (Exception e) {
            log.error("Error obtaining info from OPCUA for spawning actor at " + req
                    .getInfo().getEndpointUrl() + " with error, shutting down: " + e.getMessage());
            getSelf().tell(Kill.getInstance(), self());
        }
    }

    private Actor generateActor(CapabilityImplInfo info) throws InterruptedException, ExecutionException, UaException {
        UaNode actorNode = info.getClient().getAddressSpace().getNode(info.getActorNode()); //Use getNodeAsync instead?
        Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
        actor.setDisplayName(actorNode.getDisplayName().getText());
        actor.setActorName(actorNode.getBrowseName().getName());
        String id = info.getActorNode().getIdentifier().toString();
        String uri = info.getEndpointUrl().endsWith("/") ? info.getEndpointUrl() + id : info.getEndpointUrl() + "/" + id;
        actor.setUri(uri);
        actor.setID(uri); // reuse uri as Id as actornode identifier is not unique across machines
        return actor;
    }

    private Actor generateOutputActor(CapabilityImplInfo info, FoldingOPCUAnodes nodeIds) {
        String endpoint = resolveOutputStationEndpoint(info, nodeIds);
        if (endpoint != null) {

            Actor actor = ActorCoreModelFactory.eINSTANCE.createActor();
            String pos = transportPositionParser.parseLastIPPos(info.getEndpointUrl()).getPos();
            String id = "1";
            //String id = "TransitStation";
            actor.setDisplayName(id);
            actor.setActorName(id);
            String uri = endpoint.endsWith("/") ? endpoint + id : endpoint + "/" + id;
            actor.setUri(uri);
            actor.setID(uri);
            return actor;
        }
        return null;
    }

    /*private void spawnNewActor(CapabilityImplInfo info, Actor model, FoldingOPCUAnodes nodeIds) {
        AbstractCapability capability = WellknownFoldingCapability.getFoldingShapeCapability();
        IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
        TransportRoutingInterface.Position selfPos = resolvePosition(info);
        final ActorSelection eventBusByRef = context().actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        FoldingOPCUAWrapper hal = new FoldingOPCUAWrapper(intraEventBus, info.getClient(), info.getActorNode(), nodeIds.stopMethod, nodeIds.resetMethod, nodeIds.stateVar, nodeIds.foldMethod, getSelf());
        machine = this.context().actorOf(FoldingStationActor.props(eventBusByRef, capability, model, hal, intraEventBus), model.getActorName() + selfPos.getPos());
        log.info("Spawned Actor: " + machine.path());
    }*/

    private void spawnNewActor(CapabilityImplInfo info, Actor model, Actor outputModel, FoldingOPCUAnodes nodeIds) {
        AbstractCapability capability = WellknownFoldingCapability.getFoldingShapeCapability();
        IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
        TransportRoutingInterface.Position selfPos = resolvePosition(info);
        final ActorSelection eventBusByRef = context().actorSelection("/user/" + transportPositionParser.getLookupPrefix() + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        FoldingOPCUAWrapper hal = new FoldingOPCUAWrapper(intraEventBus, info.getClient(), info.getActorNode(), nodeIds.stopMethod, nodeIds.resetMethod, nodeIds.stateVar, nodeIds.foldMethod, getSelf());
        machine = this.context().actorOf(FoldingStationActor.props(eventBusByRef, capability, model, outputModel, hal, intraEventBus), model.getActorName() + selfPos.getPos());
        log.info("Spawned Actor: " + machine.path());
    }

    private FoldingOPCUAnodes retrieveNodeIds(CapabilityImplInfo info) throws Exception {
        //List<Node> nodes = info.getClient().getAddressSpace().browse(info.getActorNode());
        List<ReferenceDescription> referenceDescriptions = info.getClient().getAddressSpace().browse(info.getActorNode());
        // we assume unique node names and method names within this hierarchy level (thus no two capabilities with overlapping browse names)
        FoldingOPCUAnodes nodeIds = new FoldingOPCUAnodes();
        NamespaceTable namespaceTable = info.getClient().getNamespaceTable(); // Apparently necessary for retrieving NodeId
        for (ReferenceDescription r : referenceDescriptions) {
            log.info("Checking node: " + r.getBrowseName().toParseableString());
            String bName = r.getBrowseName().getName();
            if (bName == null) {  //We should be fine without this, but just to be safe
                continue;
            }
            if (bName.equalsIgnoreCase(OPCUABasicMachineBrowsenames.RESET_REQUEST))
                nodeIds.setResetMethod(r.getNodeId().toNodeIdOrThrow(namespaceTable));
            else if (bName.equalsIgnoreCase(OPCUABasicMachineBrowsenames.STOP_REQUEST))
                nodeIds.setStopMethod(r.getNodeId().toNodeIdOrThrow(namespaceTable));
            else if (bName.equalsIgnoreCase(WellknownFoldingCapability.OPCUA_FOLD_REQUEST))
                nodeIds.setFoldMethod(r.getNodeId().toNodeIdOrThrow(namespaceTable));
            else if (bName.equalsIgnoreCase(OPCUABasicMachineBrowsenames.STATE_VAR_NAME))
                nodeIds.setStateVar(r.getNodeId().toNodeIdOrThrow(namespaceTable));
            else if (bName.contains("ClientSideHandshake")) {
                retrieveOutputEndpointInfoFromHandshake(info, nodeIds, r);
            }
            //else if (bName.equalsIgnoreCase(OPCUACapabilitiesAndWiringInfoBrowsenames.REMOTE_ENDPOINT)) {
            //    nodeIds.setOutputEndpointInfo(r.getNodeId().toNodeIdOrThrow(namespaceTable));
            //}
        }
        return nodeIds;
    }

    private FoldingOPCUAnodes retrieveOutputEndpointInfoFromHandshake(CapabilityImplInfo info, FoldingOPCUAnodes nodeIds,
                                                                      ReferenceDescription clientHSRef) throws Exception {
        NamespaceTable namespaceTable = info.getClient().getNamespaceTable();
        List<ReferenceDescription> hsRefs = info.getClient().getAddressSpace()
                .browse(clientHSRef.getNodeId().toNodeIdOrThrow(namespaceTable));
        for (ReferenceDescription r : hsRefs) {
            String bName = r.getBrowseName().getName();
            if (bName == null) {  //We should be fine without this, but just to be safe
                continue;
            }
            if (bName.equalsIgnoreCase(OPCUACapabilitiesAndWiringInfoBrowsenames.WIRING_INFO)) {
                log.info("Found WiringInfo!");
                List<ReferenceDescription> wiringRefs = info.getClient().getAddressSpace()
                        .browse(r.getNodeId().toNodeIdOrThrow(namespaceTable));
                for (ReferenceDescription wRef : wiringRefs) {
                    String wName = wRef.getBrowseName().getName();
                    if (wName == null) {  //We should be fine without this, but just to be safe
                        continue;
                    }
                    if (wName.equalsIgnoreCase(OPCUACapabilitiesAndWiringInfoBrowsenames.REMOTE_ENDPOINT)) {
                        log.info("Found output endpoint!");
                        UaVariableNode endpointNode = info.getClient().getAddressSpace()
                                .getVariableNode(wRef.getNodeId().toNodeIdOrThrow(namespaceTable));
                        String endpoint = endpointNode.readValue().getValue().getValue().toString();
                        log.info("OutputStation Endpoint is :" + endpoint);
                        nodeIds.setOutputEndpointInfo(wRef.getNodeId().toNodeIdOrThrow(namespaceTable));
                        return nodeIds;
                    }
                }
            }
        }
        return nodeIds;
    }

    private TransportRoutingInterface.Position resolvePosition(CapabilityImplInfo info) {
        TransportRoutingInterface.Position pos = transportPositionParser.parseLastIPPos(info.getEndpointUrl());
        if (pos == TransportRoutingInterface.UNKNOWN_POSITION || pos.getPos().equals("1")) {
            log.error("Unable to resolve position for uri via IP Addr, trying now via Port: " + info.getEndpointUrl());
            pos = transportPositionParser.parsePosViaPortNr(info.getEndpointUrl());
            if (pos == TransportRoutingInterface.UNKNOWN_POSITION) {
                log.error("Unable to resolve position for uri via port, assigning default position 32: " + info.getEndpointUrl());
                pos = new TransportRoutingInterface.Position("32");
            }
        }
        return pos;
    }

    private String resolveOutputStationEndpoint(CapabilityImplInfo info, FoldingOPCUAnodes nodes) {
        try {
            UaVariableNode endpointNode = info.getClient().getAddressSpace().getVariableNode(nodes.getOutputEndpointInfo());
            return endpointNode.readValue().getValue().getValue().toString();
        } catch (UaException e) {
            e.printStackTrace();
            System.out.println("Could not resolve output station endpoint for FoldingStation, continuing without one");
        }
        return null;
    }

    public static class FoldingOPCUAnodes {
        NodeId stopMethod;
        NodeId resetMethod;
        NodeId stateVar;
        NodeId foldMethod;
        NodeId outputEndpointInfo;

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

        public NodeId getFoldMethod() {
            return foldMethod;
        }

        public void setFoldMethod(NodeId foldMethod) {
            this.foldMethod = foldMethod;
        }

        public boolean isComplete() {
            return (stopMethod != null && resetMethod != null && stateVar != null && foldMethod != null);
        }

        public NodeId getOutputEndpointInfo() {
            return outputEndpointInfo;
        }

        public void setOutputEndpointInfo(NodeId outputEndpointInfo) {
            this.outputEndpointInfo = outputEndpointInfo;
        }

        @Override
        public String toString() {
            String stop = stopMethod != null ? stopMethod.toParseableString() : "NULL";
            String reset = resetMethod != null ? resetMethod.toParseableString() : "NULL";
            String state = stateVar != null ? stateVar.toParseableString() : "NULL";
            String fold = foldMethod != null ? foldMethod.toParseableString() : "NULL";
            String out = outputEndpointInfo != null ? outputEndpointInfo.toParseableString() : "NULL";
            return "FoldingOPCUAnodes [stopMethod=" + stop + ", resetMethod=" + reset + ", stateVar="
                    + state + ", foldMethod=" + fold + ", outputEndpointInfo=" + out + "]";
        }


    }
}
