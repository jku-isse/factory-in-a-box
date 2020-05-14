package fiab.mes.opcuawrappers;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.iostation.wrapper.LocalIOStationActorSpawner;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.transport.actor.transportmodule.wrapper.LocalTransportModuleActorSpawner;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.opcua.CapabilityImplementationMetadata;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;

class TestIOandTurntableOPCUAIntegration {

	private static final Logger logger = LoggerFactory.getLogger(TestIOandTurntableOPCUAIntegration.class);
	
	ActorRef machineEventBus;
	ActorSystem system;

	@BeforeEach
	void setup() throws Exception{
		system = ActorSystem.create("TEST_ROOT_SYSTEM");
		// assume OPCUA server (mock or otherwise is started
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	
	}
	
	//TODO check tests!

	@Test
	void testHandoverWithVirtualIOStationsAndTT() {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
							
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(IOStationCapability.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(IOStationCapability.OUTPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});								
										
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalTransportModuleActorSpawner.props());
					}
				});
				
				String endpointURL1 = "opc.tcp://localhost:4840/milo"; //Pos34
				// we provided wiring info to TT1 for outputstation at SOUTH_CLIENT for testing purpose, for two turntable setup needs changing
				String endpointURL2 = "opc.tcp://localhost:4847/milo";	// POS SOUTH 37				
				String endpointURL3 = "opc.tcp://localhost:4843/milo";		// Pos20
				ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL1, capURI2Spawning), getRef());
				ActorRef discovAct2 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct2.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL2, capURI2Spawning), getRef());
				ActorRef discovAct3 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct3.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL3, capURI2Spawning), getRef());
				
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
				
				boolean didReactOnIdle = false;
				boolean doRun = true;				
				while (machines.size() < 3 || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {						
						machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());						
					}
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(BasicMachineStates.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						}
						else if (msue.getStatus().equals(BasicMachineStates.IDLE) && !didReactOnIdle) {
							logger.info("Sending TEST transport request to: "+msue.getMachineId());
							TransportModuleRequest req = new TransportModuleRequest(machines.get(msue.getMachineId()), new Position("34"), new Position("37"), "Order1", "TReq1");
							machines.get(msue.getMachineId()).getAkkaActor().tell(req, getRef());
							didReactOnIdle = true;
						} else if (msue.getStatus().equals(BasicMachineStates.COMPLETE) || msue.getStatus().equals(BasicMachineStates.COMPLETING)) {
							logger.info("Completing test upon receiving COMPLETE/ING from: "+msue.getMachineId());
							doRun = false;
						}
					}

				}
			}};
	}
	
	@Test
	void testHandoverWithRealIOandVirtualTT() {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(IOStationCapability.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(IOStationCapability.OUTPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});									
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalTransportModuleActorSpawner.props());
					}
				});
				
				String endpointURL1 = "opc.tcp://192.168.0.34:4840"; //Pos34
				// we provided wiring info to TT1 for outputstation at SOUTH_CLIENT for testing purpose, for two turntable setup needs changing
				String endpointURL2 = "opc.tcp://192.168.0.37:4840";	// POS SOUTH 37				
				String endpointURL3 = "opc.tcp://localhost:4843/milo";		// Pos20
				ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL1, capURI2Spawning), getRef());
				ActorRef discovAct2 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct2.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL2, capURI2Spawning), getRef());
				ActorRef discovAct3 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct3.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL3, capURI2Spawning), getRef());
				
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
				
				boolean didReactOnIdle = false;
				boolean doRun = true;				
				while (machines.size() < 3 || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {						
						machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());						
					}
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(BasicMachineStates.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						}
						else if (msue.getStatus().equals(BasicMachineStates.IDLE) && !didReactOnIdle) {
							logger.info("Sending TEST transport request to: "+msue.getMachineId());
							TransportModuleRequest req = new TransportModuleRequest(machines.get(msue.getMachineId()), new Position("34"), new Position("37"), "Order1", "TReq1");
							machines.get(msue.getMachineId()).getAkkaActor().tell(req, getRef());
							didReactOnIdle = true;
						} else if (msue.getStatus().equals(BasicMachineStates.COMPLETE) || msue.getStatus().equals(BasicMachineStates.COMPLETING)) {
							logger.info("Completing test upon receiving COMPLETE/ING from: "+msue.getMachineId());
							doRun = false;
						}
					}

				}
			}};
	}
	
	@Test
	void testHandoverWithVirtualIOStationsAndTTandRealPlotter() {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
							
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
				
				String endpointURL1 = "opc.tcp://localhost:4840/milo"; //Pos34
				// we provided wiring info to TT1 for outputstation at SOUTH_CLIENT for testing purpose, for two turntable setup needs changing
				String endpointURL2 = "opc.tcp://localhost:4847/milo";	// POS SOUTH 37				
				String endpointURL3 = "opc.tcp://localhost:4843/milo";		// Pos20
				String endpointURL4 = "opc.tcp://192.168.0.31/";
				ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL1, capURI2Spawning), getRef());
				ActorRef discovAct2 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct2.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL2, capURI2Spawning), getRef());
				ActorRef discovAct3 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct3.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL3, capURI2Spawning), getRef());
				ActorRef discovAct4 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct4.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL4, capURI2Spawning), getRef());
				
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
				
				boolean didReactOnIdle = false;
				boolean doRun = true;
				boolean plotterReady = false;
				boolean turntableReady = false;
				while (machines.size() < 4 || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {						
						machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());						
					}
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(BasicMachineStates.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						} else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("opc.tcp://192.168.0.31/119") ) {							
							plotterReady = true;
							sendPlotRequest(machines.get(msue.getMachineId()).getAkkaActor(), getRef());
						} else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("Turntable1/Turntable_FU") ) {
							turntableReady = true;
						} else if (msue.getStatus().equals(BasicMachineStates.COMPLETING) &&
								msue.getMachineId().equals("opc.tcp://192.168.0.31/119") ) {
							//now do unloading
							sendTransportRequestNorth31ToSouth37(machines.get("Turntable1/Turntable_FU"), getRef());							
						} 
					}
					if (te instanceof IOStationStatusUpdateEvent) {
						IOStationStatusUpdateEvent iosue = (IOStationStatusUpdateEvent)te;
						if ((iosue.getStatus().equals(ServerSideStates.COMPLETE) || iosue.getStatus().equals(ServerSideStates.COMPLETING)) &&
								iosue.getMachineId().equals("OutputStation/HANDSHAKE_FU") ) {
							logger.info("Completing test upon receiving COMPLETE/ING from: "+iosue.getMachineId());
							doRun = false;
						}						
					}
					if (plotterReady && turntableReady && !didReactOnIdle) {
						logger.info("Sending TEST transport request to Turntable1");
						sendTransportRequestWest34ToNorth31(machines.get("Turntable1/Turntable_FU"), getRef());
						didReactOnIdle = true;
					}
				}
			}};
	}
	
	private void sendPlotRequest(ActorRef plotter, ActorRef self) {
		LockForOrder lfo = new LockForOrder("Step1", "Order1");
		plotter.tell(lfo, self);
	}
	
	private void sendTransportRequestWest34ToNorth31(AkkaActorBackedCoreModelAbstractActor tt, ActorRef self) {
		TransportModuleRequest req = new TransportModuleRequest(tt, new Position("34"), new Position("31"), "Order1", "TReq1");
		tt.getAkkaActor().tell(req, self);
	}
	
	private void sendTransportRequestNorth31ToSouth37(AkkaActorBackedCoreModelAbstractActor tt, ActorRef self) {
		TransportModuleRequest req = new TransportModuleRequest(tt, new Position("31"), new Position("37"), "Order1", "TReq2");
		tt.getAkkaActor().tell(req, self);
	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}


}
