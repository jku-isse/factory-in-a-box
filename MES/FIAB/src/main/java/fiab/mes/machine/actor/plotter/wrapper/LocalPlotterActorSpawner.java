package fiab.mes.machine.actor.plotter.wrapper;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.ServiceFaultListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager.SubscriptionListener;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Kill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.machine.plotter.IntraMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.opcua.CapabilityImplInfo;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class LocalPlotterActorSpawner extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	ActorRef machine;
	ActorRef discovery;
	
	public static Props props() {
		return Props.create(LocalPlotterActorSpawner.class, () -> new LocalPlotterActorSpawner());
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
		if (!uri.startsWith(WellknownPlotterCapability.PLOTTING_CAPABILITY_BASE_URI))	{
			log.error("Called with nonsupported Capability: "+uri);
			return;
		}
		try {
			PlotterOPCUAnodes nodeIds = retrieveNodeIds(req.getInfo());
			if (!nodeIds.isComplete()) {
				log.error("Error obtaining methods and variables from OPCUA for spawning actor, shutting down: "+nodeIds.toString());
				getSelf().tell(Kill.getInstance(), self());	
				return;
			}
			Actor model = generateActor(req.getInfo());
			spawnNewActor(req.getInfo(), model, nodeIds);
		} catch(Exception e) {
			log.error("Error obtaining info from OPCUA for spawning actor with error, shutting down: "+e.getMessage());
			getSelf().tell(Kill.getInstance(), self());			
		}
	}
	
	private void spawnNewActor(CapabilityImplInfo info, Actor model, PlotterOPCUAnodes nodeIds) {
		Optional<SupportedColors> color = extractColor(info.getCapabilityURI());
		if (color.isPresent()) {
			AbstractCapability capability = WellknownPlotterCapability.getColorPlottingCapability(color.get());
			IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
			Position selfPos = resolvePosition(info);
			final ActorSelection eventBusByRef = context().actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
			PlotterOPCUAWrapper hal = new PlotterOPCUAWrapper(intraEventBus,  info.getClient(), info.getActorNode(), nodeIds.stopMethod, nodeIds.resetMethod, nodeIds.stateVar, nodeIds.plotMethod, getSelf());
			machine = this.context().actorOf(BasicMachineActor.props(eventBusByRef, capability, model, hal, intraEventBus), model.getActorName()+selfPos.getPos());
			log.info("Spawned Actor: "+machine.path());
		} else {
			log.error("Cannot instantiate actor with unsupported color for plotting capability");
			getSelf().tell(Kill.getInstance(), self());
		}
	}	
	
	private Optional<SupportedColors> extractColor(String uri) {
		int posLastSlash = uri.lastIndexOf("/");
		if (posLastSlash == -1) return Optional.empty();
		String colorStr = "";
		try {
			colorStr = uri.substring(posLastSlash+1);
			return Optional.of(SupportedColors.valueOf(colorStr.toUpperCase(Locale.ROOT)));			
		} catch (Exception e) {
			log.warning("Unable to parse supported color "+colorStr);
			return Optional.empty();
		}		
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
		String uri = info.getEndpointUrl().endsWith("/") ? info.getEndpointUrl()+id : info.getEndpointUrl()+"/"+id;
		actor.setUri(uri);
		actor.setID(uri); // reuse uri as Id as actornode identifier is not unique across machines
		return actor;
	}
	
	private PlotterOPCUAnodes retrieveNodeIds(CapabilityImplInfo info) throws InterruptedException, ExecutionException {
		List<Node> nodes = info.getClient().getAddressSpace().browse(info.getActorNode()).get();		
		// we assume unique node names and method names within this hierarchy level (thus no two capabilities with overlapping browse names)
		PlotterOPCUAnodes nodeIds = new PlotterOPCUAnodes();
		for (Node n : nodes) {
			log.info("Checking node: "+n.getBrowseName().get().toParseableString());						
			String bName = n.getBrowseName().get().getName();
			if (bName.equalsIgnoreCase(OPCUABasicMachineBrowsenames.RESET_REQUEST))
				nodeIds.setResetMethod(n.getNodeId().get());
			else if (bName.equalsIgnoreCase(OPCUABasicMachineBrowsenames.STOP_REQUEST))
				nodeIds.setStopMethod(n.getNodeId().get());
			else if (bName.equalsIgnoreCase(WellknownPlotterCapability.OPCUA_PLOT_REQUEST))
				nodeIds.setPlotMethod(n.getNodeId().get());
			else if (bName.equalsIgnoreCase(OPCUABasicMachineBrowsenames.STATE_VAR_NAME))
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
