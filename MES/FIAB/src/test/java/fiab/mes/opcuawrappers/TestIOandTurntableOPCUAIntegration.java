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
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.iostation.wrapper.LocalIOStationActorSpawner;
import fiab.mes.machine.actor.plotter.WellknownPlotterCapability;
import fiab.mes.machine.actor.plotter.wrapper.LocalPlotterActorSpawner;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.opcua.CapabilityImplementationMetadata;
import fiab.mes.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability;
import fiab.mes.transport.actor.transportmodule.wrapper.LocalTransportModuleActorSpawner;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.transport.msg.InternalTransportModuleRequest;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

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

	@Test
	void testHandoverWithVirtualIOStationsAndTT() {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
							
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(HandshakeProtocol.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(HandshakeProtocol.OUTPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});								
										
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownTransportModuleCapability.TURNTABLE_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
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
						if (msue.getStatus().equals(MachineStatus.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						}
						else if (msue.getStatus().equals(MachineStatus.IDLE) && !didReactOnIdle) {
							logger.info("Sending TEST transport request to: "+msue.getMachineId());
							TransportModuleRequest req = new TransportModuleRequest(machines.get(msue.getMachineId()), new Position("34"), new Position("37"), "Order1", "TReq1");
							machines.get(msue.getMachineId()).getAkkaActor().tell(req, getRef());
							didReactOnIdle = true;
						} else if (msue.getStatus().equals(MachineStatus.COMPLETE) || msue.getStatus().equals(MachineStatus.COMPLETING)) {
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
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(HandshakeProtocol.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(HandshakeProtocol.OUTPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});									
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownTransportModuleCapability.TURNTABLE_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
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
						if (msue.getStatus().equals(MachineStatus.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						}
						else if (msue.getStatus().equals(MachineStatus.IDLE) && !didReactOnIdle) {
							logger.info("Sending TEST transport request to: "+msue.getMachineId());
							TransportModuleRequest req = new TransportModuleRequest(machines.get(msue.getMachineId()), new Position("34"), new Position("37"), "Order1", "TReq1");
							machines.get(msue.getMachineId()).getAkkaActor().tell(req, getRef());
							didReactOnIdle = true;
						} else if (msue.getStatus().equals(MachineStatus.COMPLETE) || msue.getStatus().equals(MachineStatus.COMPLETING)) {
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
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
							
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(HandshakeProtocol.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(HandshakeProtocol.OUTPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});																	
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownTransportModuleCapability.TURNTABLE_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalTransportModuleActorSpawner.props());
					}
				});
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownPlotterCapability.PLOTTING_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalPlotterActorSpawner.props());
					}
				});
				
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
						if (msue.getStatus().equals(MachineStatus.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						} else if (msue.getStatus().equals(MachineStatus.IDLE) && msue.getMachineId().equals("opc.tcp://192.168.0.31/119") ) {							
							plotterReady = true;
							sendPlotRequest(machines.get(msue.getMachineId()).getAkkaActor(), getRef());
						} else if (msue.getStatus().equals(MachineStatus.IDLE) && msue.getMachineId().equals("Turntable1/Turntable_FU") ) {
							turntableReady = true;
						} else if (msue.getStatus().equals(MachineStatus.COMPLETING) &&
								msue.getMachineId().equals("opc.tcp://192.168.0.31/119") ) {
							//now do unloading
							sendTransportRequestNorth31ToSouth37(machines.get("Turntable1/Turntable_FU"), getRef());							
						} 
					}
					if (te instanceof IOStationStatusUpdateEvent) {
						IOStationStatusUpdateEvent iosue = (IOStationStatusUpdateEvent)te;
						if ((iosue.getStatus().equals(ServerSide.COMPLETE) || iosue.getStatus().equals(ServerSide.COMPLETING)) &&
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