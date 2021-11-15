package fiab.mes.order;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import fiab.mes.transport.msg.TransportSystemStatusMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.shopfloor.DefaultLayout;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;

class OrderPlanningActorTest {

	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "routes";	
	protected static ActorRef orderEventBus;
	protected static ActorRef orderPlanningActor;
	protected static ActorRef coordActor;
	protected static DefaultLayout layout;
	
	private static final Logger logger = LoggerFactory.getLogger(OrderPlanningActorTest.class);
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
		layout = new DefaultLayout(system);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
		orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);

		
	}

	@AfterAll
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@BeforeEach
	public void setupBeforeEach() {
		knownActors.clear();
	}
	
	
	@Test //WORKS
	void testInitOrderPlannerWithTransport() throws Exception {
		new TestKit(system) { 
			{ 															
				String oid = "Order1";
				layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );							
				layout.setupTwoTurntableWith2MachinesAndIO();				
				orderEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", oid)), getRef() );
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				while (!isPlannerFunctional || countConnEvents < 8 ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class); 
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
					}
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED)) 
							getLastSender().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef());
					}
//					if (te instanceof IOStationStatusUpdateEvent) {
//						if (((IOStationStatusUpdateEvent) te).getStatus().equals(ServerSide.Stopped)) 
//							getLastSender().tell(new GenericMachineRequests.Reset(((IOStationStatusUpdateEvent) te).getMachineId()), getRef());
//					}
				} 
			}	
		};
	}
	
	@Test //WORKS
	void testInitOrderPlannerWithSingleStepProcessAndTransport() throws Exception {
		new TestKit(system) { 
			{ 															
				String oid = "Order1";
				layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );							
				layout.setupTwoTurntableWith2MachinesAndIO();	
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				while (!isPlannerFunctional || countConnEvents < 8 ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class, TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
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
				subscribeAndRegisterSinglePrintRedOrder(oid, getRef());
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
	
	@Test //FIXME: final transport to outputstation doesn work
	void testInitOrderPlannerWithMultipleSingleStepProcessesAndTransport() throws Exception {
		new TestKit(system) { 
			{ 															
				layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );							
				layout.setupTwoTurntableWith2MachinesAndIO();	
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				while (!isPlannerFunctional || countConnEvents < 8 ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class); 
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
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
				String oid2 = "Order2";
				subscribeAndRegisterSinglePrintRedOrder(oid1, getRef());
				subscribeAndRegisterSinglePrintRedOrder(oid2, getRef());
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
	
	@Test // WORKS
	void testInitOrderPlannerWithMultipleParallelSingleStepProcessesAndTransport() throws Exception {
		new TestKit(system) { 
			{ 															
				layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );							
				layout.setupTwoTurntableWith2MachinesAndIO();	
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				while (!isPlannerFunctional || countConnEvents < 8 ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class, TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
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
				String oid2 = "Order2";
				subscribeAndRegisterSinglePrintRedOrder(oid1, getRef());
				subscribeAndRegisterSinglePrintGreenOrder(oid2, getRef());
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

	@Test //FIXME next order starts with id null, foldingstation never enters execute
	void testInitOrderPlannerWithSingleStepFoldProcessAndTransport() throws Exception {
		new TestKit(system) {
			{
				String oid = "Order1";
				layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				layout.setupTwoTurntableWith2FoldingStationsAndIO();
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				while (!isPlannerFunctional || countConnEvents < 8 ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class, TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						isPlannerFunctional = true;
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
				subscribeAndRegisterSingleFoldBoxOrder(oid, getRef());
				boolean orderDone = false;
				while (!orderDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), TimedEvent.class);
					logEvent(te);
					if (te instanceof OrderEvent &&
							(((OrderEvent) te).getEventType().equals(OrderEvent.OrderEventType.REMOVED) ||
									((OrderEvent)te).getEventType().equals(OrderEvent.OrderEventType.COMPLETED))) {
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
	
	public void subscribeAndRegisterSinglePrintRedOrder(String oid, ActorRef testProbe) {		
		orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe );
		OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleRedStepProcess(oid));				
		RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
		orderPlanningActor.tell(req, testProbe);
	}
	
	public void subscribeAndRegisterSinglePrintGreenOrder(String oid, ActorRef testProbe) {		
		orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe );
		OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleGreenStepProcess(oid));				
		RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
		orderPlanningActor.tell(req, testProbe);
	}

	public void subscribeAndRegisterSingleFoldBoxOrder(String oid, ActorRef testProbe) {
		orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe );
		OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleFoldBoxProcess(oid));
		RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
		orderPlanningActor.tell(req, testProbe);
	}
	
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}


}
