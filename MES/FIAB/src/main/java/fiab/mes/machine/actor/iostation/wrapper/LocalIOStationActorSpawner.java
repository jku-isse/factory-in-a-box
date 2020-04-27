package fiab.mes.machine.actor.iostation.wrapper;

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
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface.CapabilityImplInfo;

public class LocalIOStationActorSpawner extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	public static String nsPrefix = "2:";
	
	ActorRef machine;
	
	public static Props props() {
		return Props.create(LocalIOStationActorSpawner.class, () -> new LocalIOStationActorSpawner());
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
		boolean isInputStation = false;
		if (uri.equalsIgnoreCase(IOStationCapability.INPUTSTATION_CAPABILITY_URI))
			isInputStation = true;
		else if (uri.equalsIgnoreCase(IOStationCapability.OUTPUTSTATION_CAPABILITY_URI)) {
			isInputStation = false;
		} else { // something else, abort 
			log.error("Called with nonsupported Capability: "+uri);
			return;
		}
		try {
			IOStationOPCUAnodes nodeIds = retrieveNodeIds(req.getInfo());
			if (!nodeIds.isComplete()) {
				log.error("Error obtaining methods and variables from OPCUA for spawning actor: "+nodeIds.toString());
				return;
			}
			Actor model = generateActor(req.getInfo());
			spawnNewIOStationActor(req.getInfo(), isInputStation, model, nodeIds.getStopMethod(), nodeIds.getResetMethod(), nodeIds.getStateVar());
		} catch(Exception e) {
			log.error("Error obtaining info from OPCUA for spawning actor with error: "+e.getMessage());
		}
	}
	
	private void spawnNewIOStationActor(CapabilityImplInfo info, boolean isInputStation, Actor model, NodeId stopMethod, NodeId resetMethod, NodeId stateVar) {
		final ActorSelection eventBusByRef = context().actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		AbstractCapability capability = isInputStation ? IOStationCapability.getInputStationCapability() : IOStationCapability.getOutputStationCapability();
		InterMachineEventBus intraEventBus = new InterMachineEventBus();
		IOStationOPCUAWrapper wrapper = new IOStationOPCUAWrapper(intraEventBus, info.getClient(), info.getActorNode(), stopMethod, resetMethod, stateVar);
		machine = this.context().actorOf(BasicIOStationActor.props(eventBusByRef, capability, model, wrapper, intraEventBus), model.getActorName());
		
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
	
	private IOStationOPCUAnodes retrieveNodeIds(CapabilityImplInfo info) throws InterruptedException, ExecutionException {
		List<Node> nodes = info.getClient().getAddressSpace().browse(info.getActorNode()).get();		
		// we assume unique node names and method names within this hierarchy level (thus no two capabilities with overlapping browse names)
		IOStationOPCUAnodes nodeIds = new IOStationOPCUAnodes();
		for (Node n : nodes) {
			log.info("Checking node: "+n.getBrowseName().get().toParseableString());
			if (n instanceof UaVariableNode) {				
				String bName = n.getBrowseName().get().getName();
				switch(bName) {				
				case IOStationCapability.STATE_VAR_NAME:
					nodeIds.setStateVar(n.getNodeId().get());
					break;
				}				
			}
			if (n instanceof UaMethodNode) {				
				String bName = n.getBrowseName().get().getName();
				switch(bName) {
				case IOStationCapability.RESET_REQUEST:
					nodeIds.setResetMethod(n.getNodeId().get());
					break;
				case IOStationCapability.STOP_REQUEST:
					nodeIds.setStopMethod(n.getNodeId().get());
					break;				
				}				
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
