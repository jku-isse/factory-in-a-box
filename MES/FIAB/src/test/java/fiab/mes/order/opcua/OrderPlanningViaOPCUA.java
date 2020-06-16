package fiab.mes.order.opcua;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
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
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.opcua.CapabilityImplementationMetadata;

class OrderPlanningViaOPCUA {

	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "routes";	
	protected static ActorRef orderEventBus;
	protected static ActorRef orderPlanningActor;
	protected static ActorRef coordActor;
	protected static ActorRef machineEventBus;
	
	private static final Logger logger = LoggerFactory.getLogger(OrderPlanningViaOPCUA.class);
	static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
	
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		// setup shopfloor
		// setup machines
		// setup processes
		// setup order actors?
		// add processes to orderplanning actor
		system = ActorSystem.create(ROOT_SYSTEM);
		HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
		TransportPositionLookup dns = new TransportPositionLookup();		
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
		orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
				
	}

	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Before
	public static void setupBeforeEach() {
		knownActors.clear();
	}
	
	
	@Test // FIXME: plotter transitions directly into complete instead of waiting in completing for pallet removal
	void testInitOrderPlannerWithSingleStepProcessAndTransport() throws Exception {
		new TestKit(system) { 
			{ 			
				Set<String> urlsToBrowse = new HashSet<String>();
				urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34 west inputstation
				//urlsToBrowse.add("opc.tcp://192.168.0.31:4840"); //Pos31 north plotter

				urlsToBrowse.add("opc.tcp://192.168.0.37:4840"); //Pos37 south plotter // alternative plotter
				urlsToBrowse.add("opc.tcp://192.168.0.35:4840");	// POS EAST 35/ outputstation				
				urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");		// Pos20 TT			

		        Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
		        ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
				//final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
		        machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				
				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});								
		        
				String oid = "Order1";
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				boolean isTransportFunctional = false;
				while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() || !isTransportFunctional) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class); 
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
					}
					if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState().equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
						 isTransportFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
						knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
					}
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED)) 
							Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId() ) ).ifPresent(
									actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
							);	
					}
				} 
				subscribeAndRegisterSinglePrintBlackOrder(oid, getRef());
				
				boolean orderDone = false;
				while (!orderDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), TimedEvent.class); 
					logEvent(te);
					if (te instanceof OrderEvent && ((OrderEvent) te).getEventType().equals(OrderEvent.OrderEventType.REMOVED)) {
						orderDone = true;
					}
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED)) 
							Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId() ) ).ifPresent(
									actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
							);
					}
					
				} 
			}	
		};
	}
	
	@Test // FIXME: plotter transitions directly into complete instead of waiting in completing for pallet removal
	void testInitOrderPlannerWithTwoParallelSingleStepProcessAndTransport() throws Exception {
		new TestKit(system) { 
			{ 			
				Set<String> urlsToBrowse = new HashSet<String>();
				urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34 west inputstation
				urlsToBrowse.add("opc.tcp://192.168.0.31:4840"); //Pos31 north plotter
				//urlsToBrowse.add("opc.tcp://192.168.0.37:4840"); //Pos37 south plotter
				//urlsToBrowse.add("opc.tcp://192.168.0.35:4840");	// POS EAST 35/ outputstation				
				urlsToBrowse.add("opc.tcp://192.168.0.21:4840");	// POS EAST 35/ outputstation
				urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");		// Pos20 TT			

		        Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
		        ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
				//final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
		        machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				
				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});								
		        
				
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				boolean isTransportFunctional = false;
				while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() || !isTransportFunctional) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class); 
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
					}
					if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState().equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
						 isTransportFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
						knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
					}
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED)) 
							Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId() ) ).ifPresent(
									actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
							);	
					}
				} 
				String oid1 = "Order1";				
				subscribeAndRegisterSinglePrintBlackOrder(oid1, getRef());
				String oid2 = "Order2";
				subscribeAndRegisterSinglePrintBlackOrder(oid2, getRef());
				
				boolean order1Done = false;
				boolean order2Done = false;
				
				while (!order1Done || !order2Done) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), TimedEvent.class); 
					logEvent(te);
					if (te instanceof OrderEvent) {
						OrderEvent oe = (OrderEvent) te;
						if (oe.getEventType().equals(OrderEvent.OrderEventType.COMPLETED)) {
							System.out.println(" ---------------- Order complete: "+oe.getOrderId());
						}	
						if (oe.getEventType().equals(OrderEvent.OrderEventType.REMOVED)) {
							if (oe.getOrderId().equals(oid1)) {
								order1Done = true;
							} else if (oe.getOrderId().equals(oid2)) {
								order2Done = true;
							}
						}
					}
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED)) 
							Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId() ) ).ifPresent(
									actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
							);
					}
					
				} 
			}	
		};
	}
	

	
	public void subscribeAndRegisterSinglePrintBlackOrder(String oid, ActorRef testProbe) {		
		orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe );
		OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleBlackStepProcess(oid));				
		RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
		orderPlanningActor.tell(req, testProbe);
	}
	

	
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}


}
