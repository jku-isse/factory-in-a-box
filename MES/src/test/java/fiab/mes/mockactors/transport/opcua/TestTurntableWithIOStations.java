package fiab.mes.mockactors.transport.opcua;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fiab.turntable.TurntableFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ProcessCore.ProcessStep;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
//import fiab.machine.iostation.opcua.StartupUtil;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;

public class TestTurntableWithIOStations {

	private static final Logger logger = LoggerFactory.getLogger(TestTurntableWithIOStations.class);
	
	ActorRef machineEventBus;
	ActorSystem system;	
	ProcessStep step;
	
	public static void main(String args[]) {
		//startupW34toN31toS37();
		//startupW34toE35();
		startupW34toS37();
	}
	
	public static void startupW34toS37(){
		//FIXME
		//fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "VirtualInputStation1");
		//fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(7, "VirtualOutputStation1");
        ActorSystem system = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA");
        int portOffset = 2;
        boolean exposeInternalControls = false;
        //system.actorOf(OPCUATurntableRootActor.props("TurntableVirtualW34toS37", portOffset, exposeInternalControls), "TurntableRoot");
		TurntableFactory.startStandaloneTurntable(system, portOffset, "TurntableRoot");
	}

	public static void startupW34toN31toS37() {
		//FIXME
		//fiab.machine.iostation.opcua.StartupUtil.startupInputstation(0, "VirtualInputStation1"); //Names are reflected in Nodeset, do not change without propagating to wiringinfo.json
		//fiab.machine.iostation.opcua.StartupUtil.startupOutputstation(7, "VirtualOutputStation1");
		fiab.machine.plotter.opcua.StartupUtil.startup(5, "VirtualPlotter31", SupportedColors.BLACK);
        ActorSystem system = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA");
        int portOffset = 2;
        boolean exposeInternalControls = false;
        //system.actorOf(OPCUATurntableRootActor.props("TurntableVirtualW34toN31toS37", portOffset, exposeInternalControls), "TurntableRoot");
		TurntableFactory.startStandaloneTurntable(system, portOffset, "TurntableRoot");
	}
	
	public static void startupW34toE35() {
		// !!! Names are reflected in Nodeset, do not change without propagating to wiringinfo.json
		//FIXME
		//StartupUtil.startupInputstation(0, "VirtualInputStation1");
		//StartupUtil.startupOutputstation(1, "VirtualOutputStation1");
		fiab.machine.plotter.opcua.StartupUtil.startup(5, "VirtualPlotter31", SupportedColors.BLACK); //NORTH TT1
		fiab.machine.plotter.opcua.StartupUtil.startup(6, "VirtualPlotter32", SupportedColors.BLACK); //NORTH TT2
		ActorSystem systemTT1 = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA1");
        int portOffsetTT1 = 2;
        boolean exposeInternalControls = false;
        //systemTT1.actorOf(OPCUATurntableRootActor.props("TurntableVirtualW34toN31toE21", portOffsetTT1, exposeInternalControls), "TurntableRoot");
		TurntableFactory.startStandaloneTurntable(systemTT1, portOffsetTT1, "TurntableRoot");
        ActorSystem systemTT2 = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA2");
        int portOffsetTT2 = 3;
        //systemTT2.actorOf(OPCUATurntableRootActor.props("TurntableVirtualW20toN32toE35", portOffsetTT2, exposeInternalControls), "TurntableRoot");
		TurntableFactory.startStandaloneTurntable(systemTT2, portOffsetTT2, "TurntableRoot");
	
	}
	
	
	@BeforeEach
	void setup() throws Exception{
		system = ActorSystem.create("TEST_ROOT_SYSTEM");
		// assume OPCUA server (mock or otherwise is started
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		ProcessCore.Process p = ProduceProcess.getSequential4ColorProcess("P1-");
		OrderProcess op = new OrderProcess(p);
		op.activateProcess();
		step = op.getAvailableSteps().get(0);
	}
	
	
// WORKS
	@Test
	@Tag("IntegrationTest")
	void virtualIOandTT() {		
		// MAKE SURE TO RUN CORRECT SHOPFLOOR LAYOUT ABOVE
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://localhost:4840/milo"); //Pos34
		// we provided wiring info to TT1 for outputstation at SOUTH_CLIENT for testing purpose, for two turntable setup needs changing
		urlsToBrowse.add("opc.tcp://localhost:4847/milo");	// POS SOUTH 37				
		urlsToBrowse.add("opc.tcp://localhost:4842/milo");		// Pos20
		
		
		Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
		ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
		runTransport34to37TestWith(capURI2Spawning, urlsToBrowse);
	}
	
	@Test  //Works somewhat
	@Tag("SystemTest")
	void realIOandRealSingleTT() {
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34 west inputstation
		urlsToBrowse.add("opc.tcp://192.168.0.35:4840");	// POS EAST 35/ outputstation				
		urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");		// Pos20 TT
		Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
		ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
		Position posFrom = new Position("34");
		Position posTo = new Position("35");
		runTransportTestWith(capURI2Spawning, urlsToBrowse, posFrom, posTo);
	}
	
//	@Test  //FIXME: hardware centric not ok
//	void realIOandRealSingleTTAndPLotter() {
//		Set<String> urlsToBrowse = new HashSet<String>();
//		urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34 west inputstation
//		urlsToBrowse.add("opc.tcp://192.168.0.35:4840");	// POS EAST 35/ outputstation				
//		urlsToBrowse.add("opc.tcp://192.168.0.31:4840");	// POS NORTH 31/ plotter 1
//		urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");		// Pos20 TT
//		Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
//		ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
//		Position posFrom = new Position("34");
//		Position posTo = new Position("31");
//		runTransportTestWith(capURI2Spawning, urlsToBrowse, posFrom, posTo);
//	}
	
	private boolean runTransport34to37TestWith(Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning, Set<String> urlsToBrowse) {
		Position posFrom = new Position("34");
		Position posTo = new Position("37");
		return runTransportTestWith(capURI2Spawning, urlsToBrowse, posFrom, posTo);
	}
	
	private boolean runTransportTestWith(Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning, Set<String> urlsToBrowse, Position posFrom, Position posTo) {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				
				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});
				
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
				
				boolean didReactOnIdle = false;
				boolean doRun = true;				
				while (doRun == true) {
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
							TransportModuleRequest req = new TransportModuleRequest(machines.get(msue.getMachineId()), posFrom, posTo, "Order1", "TReq1");
							machines.get(msue.getMachineId()).getAkkaActor().tell(req, getRef());
							didReactOnIdle = true;
						} else if (msue.getStatus().equals(BasicMachineStates.COMPLETE) || msue.getStatus().equals(BasicMachineStates.COMPLETING)) {
							logger.info("Completing test upon receiving COMPLETE/ING from: "+msue.getMachineId());
							doRun = false;
						}
					}
				}
			}};
			return true;
	}

	
	
	// Works
	@Test
	@Tag("IntegrationTest")
	void testHandoverWithVirtualIOStationsAndTTandVirtualPlotter() {
		new TestKit(system) { 
			{ 
				// MAKE SURE TO RUN CORRECT SHOPFLOOR LAYOUT ABOVE
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
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
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ReadyForProcessEvent.class); 
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
							sendTransportRequestNorth31ToSouth37(machines.get("TurntableVirtualW34toN31toS37/Turntable_FU"), getRef());							
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
						sendTransportRequestWest34ToNorth31(machines.get("TurntableVirtualW34toN31toS37/Turntable_FU"), getRef());
						didReactOnIdle = true;
					}
				}
			}};
	}
	
	@Test //WORKS
	@Tag("IntegrationTest")
	void testHandoverWithVirtualIOStationsAndTwoVirtualTTs() {
		new TestKit(system) { 
			{ 
				// MAKE SURE TO RUN CORRECT SHOPFLOOR LAYOUT ABOVE
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor		
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
				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
				
				boolean didReactOnIdle = false;
				boolean doRun = true;
				boolean turntableReady1 = false;
				boolean turntableReady2 = false;
				while (machines.size() < urlsToBrowse.size() || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ReadyForProcessEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {						
						machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());						
					}
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(BasicMachineStates.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						} else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("TurntableVirtualW34toN31toE21/Turntable_FU") ) {
							turntableReady1 = true;
						} else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("TurntableVirtualW20toN32toE35/Turntable_FU") ) {
							turntableReady2 = true;
						}  
					}
					if (te instanceof IOStationStatusUpdateEvent) {
						IOStationStatusUpdateEvent iosue = (IOStationStatusUpdateEvent)te;
						if ((iosue.getStatus().equals(ServerSideStates.COMPLETE) || iosue.getStatus().equals(ServerSideStates.COMPLETING)) &&
								iosue.getMachineId().equals("VirtualOutputStation1/IOSTATION") ) {
							logger.info("Completing test upon receiving COMPLETE/ING from: "+iosue.getMachineId());
							doRun = false;
						}						
					}
					if (turntableReady1 && turntableReady2 && !didReactOnIdle) {
						logger.info("Sending TEST transport request to Turntable1");
						AkkaActorBackedCoreModelAbstractActor tt1 = machines.get("TurntableVirtualW34toN31toE21/Turntable_FU");
						TransportModuleRequest req = new TransportModuleRequest(tt1, new Position("34"), new Position("21"), "Order1", "TReq1");
						tt1.getAkkaActor().tell(req, getRef());
						logger.info("Sending TEST transport request to Turntable2");
						AkkaActorBackedCoreModelAbstractActor tt2 = machines.get("TurntableVirtualW20toN32toE35/Turntable_FU");
						TransportModuleRequest req2 = new TransportModuleRequest(tt2, new Position("20"), new Position("35"), "Order1", "TReq2");
						tt2.getAkkaActor().tell(req2, getRef());
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
