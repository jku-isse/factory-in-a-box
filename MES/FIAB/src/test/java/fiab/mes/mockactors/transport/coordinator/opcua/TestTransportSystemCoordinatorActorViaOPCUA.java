package fiab.mes.mockactors.transport.coordinator.opcua;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ProcessCore.ProcessStep;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import main.java.fiab.core.capabilities.BasicMachineStates;
import main.java.fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import main.java.fiab.core.capabilities.events.TimedEvent;
import main.java.fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.handshake.actor.ClientHandshakeActor;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.ServerSideHandshakeActor;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.mockactors.transport.opcua.TestTurntableWithIOStations;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;
import fiab.opcua.CapabilityImplementationMetadata;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.actor.TransportModuleCoordinatorActor;
import fiab.turntable.conveying.BaseBehaviorConveyorActor;
import fiab.turntable.turning.BaseBehaviorTurntableActor;

class TestTransportSystemCoordinatorActorViaOPCUA {

	private static final Logger logger = LoggerFactory.getLogger(TestTransportSystemCoordinatorActorViaOPCUA.class);
	
	public static void main(String args[]) {
		//Single TTtests:  TestTurntableWithIOStations.startupW34toN31toS37();
		// Dual TT tests:
		TestTurntableWithIOStations.startupW34toE35();
	}
	
	
	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "TEST_TRANSPORTSYSTEM";
//	protected static ActorRef machineEventBus;
	protected static ActorRef orderEventBus;
	protected static ActorRef machineEventBus;
	protected static ActorRef coordActor;
	protected static ProcessStep step;
	HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
	TransportPositionLookup dns = new TransportPositionLookup();

	static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
	
	
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {			
		system = ActorSystem.create(ROOT_SYSTEM);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		ProcessCore.Process p = ProduceProcess.getSequential4ColorProcess("P1-");
		OrderProcess op = new OrderProcess(p);
		op.activateProcess();
		step = op.getAvailableSteps().get(0);
	}

	@Before
	public static void setupBeforeEach() {
		knownActors.clear();
	}
	
	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}
	
	@Test // WORKS!!! YES
	void virtualIOandTT() {
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://localhost:4840/milo"); //Pos34
		// we provided wiring info to TT1 for outputstation at SOUTH_CLIENT for testing purpose, for two turntable setup needs changing
		urlsToBrowse.add("opc.tcp://localhost:4847/milo");	// POS SOUTH 37				
		urlsToBrowse.add("opc.tcp://localhost:4842/milo");		// Pos20
		
		
		Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
		ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
		runTransportInputToOutputStation(capURI2Spawning, urlsToBrowse);
	}
	
	@Test //WORKS
	void virtualIOandTwoTTs() {
		// !!! CAREFULL TO LOAD CORRECT LAYOUT
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://localhost:4840/milo"); //Pos34 input station
		urlsToBrowse.add("opc.tcp://localhost:4841/milo");	// POS EAST of TT2, Pos 35 output station				
		urlsToBrowse.add("opc.tcp://localhost:4842/milo");		// TT1 Pos20
		urlsToBrowse.add("opc.tcp://localhost:4843/milo");		// TT2 Pos21
		// virtual plotters
		urlsToBrowse.add("opc.tcp://localhost:4845/milo");	// POS NORTH of TT1 31		
		urlsToBrowse.add("opc.tcp://localhost:4846/milo");	// POS NORTH of TT2 32	
		Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
		ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
		runTransportInputToOutputStation(capURI2Spawning, urlsToBrowse);
	}
	
	private boolean runTransportInputToOutputStation(Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning, Set<String> urlsToBrowse) {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), "TransportCoordinator");
				
				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});
				
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
				
				
				boolean didReactOnIdle = false;
				boolean doRun = true;				
				while (machines.size() < urlsToBrowse.size() || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class); 
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
							RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("VirtualInputStation1/IOSTATION"), knownActors.get("VirtualOutputStation1/IOSTATION"), "TestOrder1", getRef());
							coordActor.tell(rtr, getRef());				
							didReactOnIdle = true;
						} 	
					}
					if (te instanceof IOStationStatusUpdateEvent) {
						
					}
					if (te instanceof RegisterTransportRequestStatusResponse) {
						RegisterTransportRequestStatusResponse rtrr = (RegisterTransportRequestStatusResponse) te;
						if (rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.COMPLETED)) {
							doRun = false;
						} else { 
							assertTrue(rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.QUEUED) ||
									rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.ISSUED)) ;
						}
					}

				}
			}};
			return true;
	}
	
	// Works
	@Test
	void testHandoverWithVirtualIOStationsAndTTandVirtualPlotter() {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), "TransportCoordinator");
				// setup discoveryactor		
				Set<String> urlsToBrowse = new HashSet<String>();
				urlsToBrowse.add("opc.tcp://localhost:4840/milo"); //Pos34
				// we provided wiring info to TT1 for outputstation at SOUTH_CLIENT for testing purpose, for two turntable setup needs changing
				urlsToBrowse.add("opc.tcp://localhost:4847/milo");	// POS SOUTH 37				
				urlsToBrowse.add("opc.tcp://localhost:4842/milo");		// Pos20
				// virtual plotter
				urlsToBrowse.add("opc.tcp://localhost:4845/milo");	// POS NORTH 31		
				
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
				
				boolean didReactOnIdle = false;
				boolean doRun = true;
				boolean plotterReady = false;
				boolean turntableReady = false;
				while (machines.size() < urlsToBrowse.size() || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ReadyForProcessEvent.class, RegisterTransportRequestStatusResponse.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {						
						machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());						
					}
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(BasicMachineStates.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						} else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU") ) {							
							sendPlotRegister(machines.get(msue.getMachineId()).getAkkaActor(), getRef());
						} else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("TurntableVirtualW34toN31toS37/Turntable_FU") ) {
							turntableReady = true;
						} else if (msue.getStatus().equals(BasicMachineStates.COMPLETING) &&
								msue.getMachineId().equals("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU") ) {
							//now do unloading
							RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU"), knownActors.get("VirtualOutputStation1/IOSTATION"), "TestOrder1", getRef());
							coordActor.tell(rtr, getRef());								
						} 
					}
					if (te instanceof ReadyForProcessEvent) {
						assert(((ReadyForProcessEvent) te).isReady());
						plotterReady = true;
						sendPlotRequest(machines.get("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU").getAkkaActor(), getRef());						
					}
					
					if (te instanceof IOStationStatusUpdateEvent) {
						IOStationStatusUpdateEvent iosue = (IOStationStatusUpdateEvent)te;
						if ((iosue.getStatus().equals(ServerSideStates.COMPLETE) || iosue.getStatus().equals(ServerSideStates.COMPLETING)) &&
								iosue.getMachineId().equals("VirtualOutputStation1/IOSTATION") ) {
							logger.info("Completing test upon receiving COMPLETE/ING from: "+iosue.getMachineId());
							doRun = false;
						}						
					}
					if (plotterReady && turntableReady && !didReactOnIdle) {
						logger.info("Sending TEST transport request to Turntable1");
						RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("VirtualInputStation1/IOSTATION"), knownActors.get("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU"), "TestOrder1", getRef());
						coordActor.tell(rtr, getRef());		
						didReactOnIdle = true;
					}
				}
			}};
	}
	
	
	
	@Test //FIXME to adapt
	void testHandoverWithRealIOStationsAndTTandPlotter() {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), "TransportCoordinator");
				// setup discoveryactor		
				Set<String> urlsToBrowse = new HashSet<String>();
				urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34 west inputstation
				urlsToBrowse.add("opc.tcp://192.168.0.31:4840"); //Pos31 north plotter
				urlsToBrowse.add("opc.tcp://192.168.0.35:4840");	// POS EAST 35/ outputstation				
				urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");		// Pos20 TT		
				
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
				
				boolean didReactOnIdle = false;
				boolean doRun = true;
				boolean plotterReady = false;
				boolean turntableReady = false;
				while (machines.size() < urlsToBrowse.size() || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ReadyForProcessEvent.class, RegisterTransportRequestStatusResponse.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {						
						machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());						
					}
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(BasicMachineStates.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						} else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU") ) {							
							sendPlotRegister(machines.get(msue.getMachineId()).getAkkaActor(), getRef());
						} else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("Turntable1/Turntable_FU") ) {
							turntableReady = true;
						} else if (msue.getStatus().equals(BasicMachineStates.COMPLETING) &&
								msue.getMachineId().equals("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU") ) {
							//now do unloading
							RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU"), knownActors.get("VirtualOutputStation1/IOSTATION"), "TestOrder1", getRef());
							coordActor.tell(rtr, getRef());								
						} 
					}
					if (te instanceof ReadyForProcessEvent) {
						assert(((ReadyForProcessEvent) te).isReady());
						plotterReady = true;
						sendPlotRequest(machines.get("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU").getAkkaActor(), getRef());						
					}
					
					if (te instanceof IOStationStatusUpdateEvent) {
						IOStationStatusUpdateEvent iosue = (IOStationStatusUpdateEvent)te;
						if ((iosue.getStatus().equals(ServerSideStates.COMPLETE) || iosue.getStatus().equals(ServerSideStates.COMPLETING)) &&
								iosue.getMachineId().equals("VirtualOutputStation1/IOSTATION") ) {
							logger.info("Completing test upon receiving COMPLETE/ING from: "+iosue.getMachineId());
							doRun = false;
						}						
					}
					if (plotterReady && turntableReady && !didReactOnIdle) {
						logger.info("Sending TEST transport request to Turntable1");
						RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("VirtualInputStation1/IOSTATION"), knownActors.get("opc.tcp://localhost:4845/milo/VirtualPlotter31/Plotting_FU"), "TestOrder1", getRef());
						coordActor.tell(rtr, getRef());		
						didReactOnIdle = true;
					}
				}
			}};
	}
	
	private void sendPlotRequest(ActorRef plotter, ActorRef self) {
		LockForOrder lfo = new LockForOrder("Step1", "Order1");
		plotter.tell(lfo, self);
	}
	
	private void sendPlotRegister(ActorRef plotter, ActorRef self) {
		RegisterProcessStepRequest req = new RegisterProcessStepRequest("Order1", "Step1", step, self);
		plotter.tell(req, self);
	}

	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
		if (event instanceof MachineConnectedEvent) {
			knownActors.put(((MachineConnectedEvent) event).getMachineId(), ((MachineConnectedEvent) event).getMachine());
		}
	}
	

	
}
