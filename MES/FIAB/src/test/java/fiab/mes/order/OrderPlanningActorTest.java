package fiab.mes.order;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockMachineActor;
import fiab.mes.mockactors.TestMockMachineActor;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;
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
	protected static ActorRef machineEventBus;
	protected static ActorRef orderEventBus;
	protected static ActorRef orderPlanningActor;
	protected static ActorRef coordActor;
	
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
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
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
	
	
	@Test
	void testInitOrderPlannerWithTransport() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{ 															
				String oid = "Order1";
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", oid)), getRef() );
							
				new DefaultLayout(system).setupTwoTurntableWith2MachinesAndIO();
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				while (!isPlannerFunctional && countConnEvents < 8 ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class); 
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
					}
				} 
			}	
		};
	}
	
	@Test
	void testInitOrderPlannerWithSingleStepProcessAndTransport() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{ 															
				String oid = "Order1";
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				//orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", oid)), getRef() );
							
				new DefaultLayout(system).setupTwoTurntableWith2MachinesAndIO();
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				while (!isPlannerFunctional && countConnEvents < 8 ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class); 
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
					}
				} 
				//ProcessCore.Process p = TestMockMachineActor.getSingleStepProcess(oid);
				//OrderProcess op = new OrderProcess(p);
				//RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op, getRef());
				//orderPlanningActor.tell(req, getRef());
				subscribeAndRegisterSinglePrintRedOrder(oid, getRef());
				boolean orderDone = false;
				while (!orderDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), TimedEvent.class); 
					logEvent(te);
					if (te instanceof OrderEvent && ((OrderEvent) te).getEventType().equals(OrderEvent.OrderEventType.REMOVED)) {
						orderDone = true;
					}
					
				} 
			}	
		};
	}
	
	@Test
	void testInitOrderPlannerWithMultipleSingleStepProcessesAndTransport() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{ 															
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );	
							
				new DefaultLayout(system).setupTwoTurntableWith2MachinesAndIO();
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				while (!isPlannerFunctional && countConnEvents < 8 ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class); 
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
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
				} 
			}	
		};
	}
	
	@Test
	void testInitOrderPlannerWithMultipleParallelSingleStepProcessesAndTransport() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{ 															
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );	
							
				new DefaultLayout(system).setupTwoTurntableWith2MachinesAndIO();
				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				while (!isPlannerFunctional && countConnEvents < 8 ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class); 
					logEvent(te);
					if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						 isPlannerFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
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
				} 
			}	
		};
	}
	
	public void subscribeAndRegisterSinglePrintRedOrder(String oid, ActorRef testProbe) {		
		orderEventBus.tell(new SubscribeMessage(testProbe, new SubscriptionClassifier("OrderMock", oid)), testProbe );
		OrderProcess op1 = new OrderProcess(TestMockMachineActor.getSingleRedStepProcess(oid));				
		RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op1, testProbe);
		orderPlanningActor.tell(req, testProbe);
	}
	
	public void subscribeAndRegisterSinglePrintGreenOrder(String oid, ActorRef testProbe) {		
		orderEventBus.tell(new SubscribeMessage(testProbe, new SubscriptionClassifier("OrderMock", oid)), testProbe );
		OrderProcess op1 = new OrderProcess(TestMockMachineActor.getSingleGreenStepProcess(oid));				
		RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op1, testProbe);
		orderPlanningActor.tell(req, testProbe);
	}
	
//	@Test
//	void testOrderRegisterWithoutAvailableMachines() {
//		new TestKit(system) { 
//			{ 
//				String oid = "Order1";
//				orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", oid)), getRef() );
//				ProcessCore.Process p = TestMockMachineActor.getSequentialProcess();
//				OrderProcess op = new OrderProcess(p);
//				RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op, getRef());
//				orderPlanningActor.tell(req, getRef());
//				Boolean regOk = expectMsgPF(Duration.ofSeconds(1), "Register Order Event expected", event -> { 
//					if (event instanceof OrderEvent) {
//						return ((OrderEvent) event).getEventType().equals(OrderEventType.REGISTERED); 
//					} else return false; });
//				assert(regOk);
//				expectMsgClass(OrderProcessUpdateEvent.class);
//				Boolean pauseOk = expectMsgPF(Duration.ofSeconds(1), "Pause Order Event expected", event -> { 
//					if (event instanceof OrderEvent) {
//						return ((OrderEvent) event).getEventType().equals(OrderEventType.PAUSED); 
//					} else return false; });
//				assert(pauseOk);
//			}	
//		};
//	}
//	
//	@Test
//	void testOrderPlanningActorReceivingMachineActorCapabilities() {
//		new TestKit(system) { 
//			{ 
//				String mid = "OrderMockMachine";
//				machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", mid)), getRef() );
//				ActorRef red1 = getMachineMockActor(1, "Red");
//				ActorRef blue2 = getMachineMockActor(2, "Blue");
//				ActorRef green3 = getMachineMockActor(3, "Green");
//				ActorRef yellow4 = getMachineMockActor(4, "Yellow");
//				//expectMsgClass(MachineUpdateEvent.class);
//				//TODO: figure out how to inspect that capabilities are stored
//			}	
//		};
//	}
//
//	
//	@Test
//	void testOrderRegisterWithAlreadyAvailableMachines() {
//		new TestKit(system) { 
//			{ 
//				String oid = "Order1";
//				String mid = "OrderMockMachine";
//				orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", oid)), getRef() );
//				machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "*")), getRef() );
//				//machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", mid)), getRef() );
//				// if we subscribe to machinebus then we need to capture all the machine events by those actors
//				ActorRef red1 = getMachineMockActor(1, "Red");
//				expectMsgAnyClassOf(Duration.ofSeconds(3), MachineConnectedEvent.class);
//				expectMsgAnyClassOf(Duration.ofSeconds(3), MachineUpdateEvent.class); //idle
//				ActorRef blue2 = getMachineMockActor(2, "Blue");
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
//				ActorRef green3 = getMachineMockActor(3, "Green");
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
//				ActorRef yellow4 = getMachineMockActor(4, "Yellow");
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
//				expectMsgClass(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
//				
//				ProcessCore.Process p = TestMockMachineActor.getSequentialProcess();
//				OrderProcess op = new OrderProcess(p);
//				RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op, getRef());
//				orderPlanningActor.tell(req, getRef());
//				Boolean regOk = expectMsgPF(Duration.ofSeconds(3600), "Register Order Event expected", event -> { 
//					if (event instanceof OrderEvent) {
//						return ((OrderEvent) event).getEventType().equals(OrderEventType.REGISTERED); 
//					} else return false; });
//				assert(regOk);
//				expectMsgClass(OrderProcessUpdateEvent.class); // First SubStep activated
//				// then order requested at machine 
//				Boolean pauseOk = expectMsgPF(Duration.ofSeconds(3600), "Pause Order Event expected", event -> { 
//					if (event instanceof OrderEvent) {
//						return ((OrderEvent) event).getEventType().equals(OrderEventType.SCHEDULED); 
//					} else return false; });
//				assert( pauseOk);
//				for (int i=0; i < 27; i++) {
//					System.out.println("i:"+i);
//					TimedEvent te = expectMsgClass(Duration.ofSeconds(3600), TimedEvent.class);
//					//logEvent(te);
//				}
//			}	
//		};
//	}
//	
//	
//	@Test
//	void testTwoOrderRegisterWithAlreadyAvailableMachines() {
//		new TestKit(system) { 
//			{ 
//				
//				String mid = "OrderMockMachine";
//				orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "*")), getRef() );
//				machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "*")), getRef() );
//				//machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", mid)), getRef() );
//				// if we subscribe to machinebus then we need to capture all the machine events by those actors
//				ActorRef red1 = getMachineMockActor(1, "Red");
//				expectMsgAnyClassOf(Duration.ofSeconds(3), MachineConnectedEvent.class);
//				expectMsgAnyClassOf(Duration.ofSeconds(3), MachineUpdateEvent.class); //idle
//				ActorRef blue2 = getMachineMockActor(2, "Blue");
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
//				ActorRef green3 = getMachineMockActor(3, "Green");
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
//				ActorRef yellow4 = getMachineMockActor(4, "Yellow");
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
//				expectMsgClass(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
//				
//				RegisterProcessRequest req = buildRequest(getRef(), "Order1", 1);
//				orderPlanningActor.tell(req, getRef());
//				RegisterProcessRequest req2 = buildRequest(getRef(), "Order2", 2);
//				orderPlanningActor.tell(req2, getRef());
//				boolean p1done = false;
//				boolean p2done = false;
//				while (!(p1done && p2done) ) {
//					TimedEvent te = expectMsgClass(Duration.ofSeconds(3600), TimedEvent.class);
//					logEvent(te);
//					if (req.getProcess().areAllTasksCancelledOrCompleted()) { p1done = true; }
//					if (req2.getProcess().areAllTasksCancelledOrCompleted()) { p2done = true; }
//					
////					if (te instanceof OrderProcessUpdateEvent) {
////						if (((OrderProcessUpdateEvent) te).getStepsWithNewStatusAsReadOnlyMap().entrySet().stream()
////							.allMatch(entry -> entry.getValue().equals(StepStatusEnum.COMPLETED))) {
////							// all steps are completed,
////							if (((OrderProcessUpdateEvent) te).getOrderId().contentEquals("Order1"))
////								p1done = true;
////							if (((OrderProcessUpdateEvent) te).getOrderId().contentEquals("Order2"))
////								p2done = true;
////						}
////						
////					}
//				}
//			}	
//		};
//	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}

	public static ActorRef getMachineMockActor(int id, String color) {
		ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		Actor modelActor = TestMockMachineActor.getDefaultMachineActor(id);
		AbstractCapability cap = TestMockMachineActor.composeInOne(TestMockMachineActor.getPlottingCapability(), TestMockMachineActor.getColorCapability(color));
		return system.actorOf(MockMachineActor.props(eventBusByRef, cap, modelActor));
	}
	
	public RegisterProcessRequest buildRequest(ActorRef senderRef, String oid, int orderCount) {
		ProcessCore.Process p = TestMockMachineActor.getSequentialProcess(orderCount+"-");
		OrderProcess op = new OrderProcess(p);
		return new RegisterProcessRequest(oid, oid, op, senderRef);
	}
}
