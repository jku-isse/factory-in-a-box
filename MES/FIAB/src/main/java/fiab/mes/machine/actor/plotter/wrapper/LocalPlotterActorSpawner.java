package fiab.mes.machine.actor.plotter.wrapper;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.WellknownPlotterCapability;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface.CapabilityImplInfo;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.handshake.HandshakeProtocol;

public class LocalPlotterActorSpawner extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	ActorRef machine;
	
	public static Props props() {
		return Props.create(LocalPlotterActorSpawner.class, () -> new LocalPlotterActorSpawner());
	}
		
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CapabilityCentricActorSpawnerInterface.SpawnRequest.class, req -> { 			
					log.info(String.format("Received CapabilityImplementationInfos for Cap: %s on %s",req.getInfo().getCapabilityURI(), req.getInfo().getEndpointUrl()));
							retrieveMethodAndVariableNodeIds(req); })
				.build();
	}

	private void retrieveMethodAndVariableNodeIds(CapabilityCentricActorSpawnerInterface.SpawnRequest req) {		
		// check if input or output station:		
		String uri = req.getInfo().getCapabilityURI();		
		if (!uri.equalsIgnoreCase(WellknownPlotterCapability.PLOTTING_CAPABILITY_URI))	{
			log.error("Called with nonsupported Capability: "+uri);
			return;
		}
		try {
			PlotterOPCUAnodes nodeIds = retrieveNodeIds(req.getInfo());
			if (!nodeIds.isComplete()) {
				log.error("Error obtaining methods and variables from OPCUA for spawning actor: "+nodeIds.toString());
				return;
			}
			Actor model = generateActor(req.getInfo());
			spawnNewActor(req.getInfo(), model, nodeIds);
		} catch(Exception e) {
			log.error("Error obtaining info from OPCUA for spawning actor with error: "+e.getMessage());
		}
	}
	
	private void spawnNewActor(CapabilityImplInfo info, Actor model, PlotterOPCUAnodes nodeIds) {
		final ActorSelection eventBusByRef = context().actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		AbstractCapability capability = WellknownTransportModuleCapability.getTurntableCapability();
		InterMachineEventBus intraEventBus = new InterMachineEventBus();
		Position selfPos = resolvePosition(info);
		PlotterOPCUAWrapper hal = new PlotterOPCUAWrapper(intraEventBus,  info.getClient(), info.getActorNode(), nodeIds.stopMethod, nodeIds.resetMethod, nodeIds.stateVar, nodeIds.plotMethod);
		machine = this.context().actorOf(BasicMachineActor.props(eventBusByRef, capability, model, hal, intraEventBus), model.getActorName()+selfPos.getPos());
		log.info("Spawned Actor: "+machine.path());
	}	
	
	private Position resolvePosition(CapabilityImplInfo info) {
		Position pos = TransportPositionLookup.parseLastIPPos(info.getEndpointUrl());
		if (pos == TransportRoutingInterface.UNKNOWN_POSITION || pos.getPos().equals("1")) { 
			log.error("Unable to resolve position for uri via IP Addr, trying now via Port: "+info.getEndpointUrl());
			pos = TransportPositionLookup.parsePosViaPortNr(info.getEndpointUrl());
			if (pos == TransportRoutingInterface.UNKNOWN_POSITION) { 
				log.error("Unable to resolve position for uri via port, assigning default position 31: "+info.getEndpointUrl());
				pos = new Position("31");
			}
		}
		return pos;
	}
	
	private Actor generateActor(CapabilityImplInfo info) throws InterruptedException, ExecutionException {
		UaNode actorNode = info.getClient().getAddressSpace().getNodeInstance(info.getActorNode()).get();
		Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
		actor.setDisplayName(actorNode.getDisplayName().get().getText());
		actor.setActorName(actorNode.getBrowseName().get().getName());
		String id = info.getActorNode().getIdentifier().toString();
		actor.setID(id);
		String uri = info.getEndpointUrl().endsWith("/") ? info.getEndpointUrl()+id : info.getEndpointUrl()+"/"+id;
		actor.setUri(uri);
		return actor;
	}
	
	private PlotterOPCUAnodes retrieveNodeIds(CapabilityImplInfo info) throws InterruptedException, ExecutionException {
		List<Node> nodes = info.getClient().getAddressSpace().browse(info.getActorNode()).get();		
		// we assume unique node names and method names within this hierarchy level (thus no two capabilities with overlapping browse names)
		PlotterOPCUAnodes nodeIds = new PlotterOPCUAnodes();
		for (Node n : nodes) {
			log.info("Checking node: "+n.getBrowseName().get().toParseableString());						
			String bName = n.getBrowseName().get().getName();
			if (bName.equalsIgnoreCase(WellknownMachinePropertyFields.MACHINE_UPCUA_RESET_REQUEST))
				nodeIds.setResetMethod(n.getNodeId().get());
			else if (bName.equalsIgnoreCase(WellknownMachinePropertyFields.MACHINE_UPCUA_STOP_REQUEST))
				nodeIds.setStopMethod(n.getNodeId().get());
			else if (bName.equalsIgnoreCase(WellknownPlotterCapability.MACHINE_UPCUA_PLOT_REQUEST))
				nodeIds.setPlotMethod(n.getNodeId().get());
			else if (bName.equalsIgnoreCase(WellknownMachinePropertyFields.STATE_VAR_NAME))
				nodeIds.setStateVar(n.getNodeId().get());											
		}
		return nodeIds;
	}
	
	public static class PlotterOPCUAnodes {
		NodeId stopMethod;
		NodeId resetMethod;
		NodeId stateVar;
		NodeId plotMethod;
		
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
		public NodeId getPlotMethod() {
			return plotMethod;
		}
		public void setPlotMethod(NodeId plotMethod) {
			this.plotMethod = plotMethod;
		}
		public boolean isComplete() {
			return (stopMethod != null && resetMethod != null && stateVar != null && plotMethod != null);
		}
		@Override
		public String toString() {
			String stop = stopMethod != null ? stopMethod.toParseableString() : "NULL";			
			String reset = resetMethod != null ? resetMethod.toParseableString() : "NULL";			
			String state = stateVar != null ? stateVar.toParseableString() : "NULL";
			String plot = plotMethod != null ? plotMethod.toParseableString() : "NULL";
			return "PlotterOPCUAnodes [stopMethod=" + stop + ", resetMethod=" + reset + ", stateVar="
					+ state + ", plotMethod=" + plot + "]";
		}
		
		
	}
	
}
