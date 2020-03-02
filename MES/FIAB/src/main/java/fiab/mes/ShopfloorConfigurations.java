package fiab.mes;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.machine.actor.iostation.wrapper.LocalIOStationActorSpawner;
import fiab.mes.machine.actor.plotter.WellknownPlotterCapability;
import fiab.mes.machine.actor.plotter.wrapper.LocalPlotterActorSpawner;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.opcua.CapabilityImplementationMetadata;
import fiab.mes.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability;
import fiab.mes.transport.actor.transportmodule.wrapper.LocalTransportModuleActorSpawner;
import fiab.mes.transport.handshake.HandshakeProtocol;

public class ShopfloorConfigurations {

	
	public static class NoDiscovery implements ShopfloorDiscovery{

		@Override
		public void triggerDiscoveryMechanism(ActorSystem system) {							
		}		
	}
	
	public static class VirtualInputOutputTurntableOnly implements ShopfloorDiscovery{

		@Override
		public void triggerDiscoveryMechanism(ActorSystem system) {
			Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
			addDefaultSpawners(capURI2Spawning);
			
			List<String> endpoints = new ArrayList<String>();
			endpoints.add("opc.tcp://localhost:4840/milo"); //Pos WEST 34
			endpoints.add("opc.tcp://localhost:4847/milo");	// POS SOUTH 37				
			endpoints.add("opc.tcp://localhost:4843/milo");		// Turntable Pos20

			endpoints.stream().forEach(ep -> createDiscoveryActor(ep, capURI2Spawning, system));					
		}		
	}
	
	public static class SingleTurntableShopfloor implements ShopfloorDiscovery{

		@Override
		public void triggerDiscoveryMechanism(ActorSystem system) {
			Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
			
			List<String> endpoints = new ArrayList<String>();
			endpoints.add("opc.tcp://192.168.0.37/"); //Pos WEST37
			endpoints.add("opc.tcp://192.168.0.34/");	// POS SOUTH 34				
			endpoints.add("opc.tcp://192.168.0.20/milo");		// Turntable Pos20
			endpoints.add("opc.tcp://192.168.0.31/");	// POS NORTH 31
			
			endpoints.stream().forEach(ep -> createDiscoveryActor(ep, capURI2Spawning, system));					
		}		
	}
	
	public static class DualTurntableShopfloor implements ShopfloorDiscovery{

		@Override
		public void triggerDiscoveryMechanism(ActorSystem system) {
			Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
			
			List<String> endpoints = new ArrayList<String>();
			endpoints.add("opc.tcp://192.168.0.37/"); //Pos WEST 37
			endpoints.add("opc.tcp://192.168.0.34/");	// POS SOUTH 34			
			endpoints.add("opc.tcp://192.168.0.20/milo");		// Turntable Pos20
			endpoints.add("opc.tcp://192.168.0.31/");	// POS NORTH 31
			
			endpoints.add("opc.tcp://192.168.0.21/milo"); // Turntable 2 Pos21
			endpoints.add("opc.tcp://192.168.0.35/"); //Pos EAST 35
			endpoints.add("opc.tcp://192.168.0.32/");	// POS NORTH 32
			endpoints.add("opc.tcp://192.168.0.38/");	// POS SOUTH 38
			
			endpoints.stream().forEach(ep -> createDiscoveryActor(ep, capURI2Spawning, system));					
		}		
	}
	
	private static void createDiscoveryActor(String endpointURL, Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning, ActorSystem system) {
		ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
		discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), ActorRef.noSender());
		
	}
	
	public static void addDefaultSpawners(Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning) {
		addInputStationSpawner(capURI2Spawning);
		addOutputStationSpawner(capURI2Spawning);
		addTurntableSpawner(capURI2Spawning);
		addPlotterStationSpawner(capURI2Spawning);
	}

	public static void addInputStationSpawner( Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning) {
		capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(HandshakeProtocol.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
			@Override
			public ActorRef createActorSpawner(ActorContext context) {
				return context.actorOf(LocalIOStationActorSpawner.props());
			}
		});
	}
	
	public static void addOutputStationSpawner( Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning) {
		capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(HandshakeProtocol.OUTPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
			@Override
			public ActorRef createActorSpawner(ActorContext context) {
				return context.actorOf(LocalIOStationActorSpawner.props());
			}
		});		
	}
	
	public static void addPlotterStationSpawner( Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning) {
		capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownPlotterCapability.PLOTTING_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
			@Override
			public ActorRef createActorSpawner(ActorContext context) {
				return context.actorOf(LocalPlotterActorSpawner.props());
			}
		});
	}
	
	public static void addTurntableSpawner( Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning) {
		capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownTransportModuleCapability.TURNTABLE_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
			@Override
			public ActorRef createActorSpawner(ActorContext context) {
				return context.actorOf(LocalTransportModuleActorSpawner.props());
			}
		});
	}
}
