<<<<<<< HEAD
package fiab.mes.order;

import java.time.Duration;
import org.junit.AfterClass;
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
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockMachineActor;
import fiab.mes.mockactors.TestMockMachineActor;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;
import fiab.mes.planer.actor.OrderPlanningActor;

class OrderPlanningActorTest {

	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "routes";
	protected static ActorRef machineEventBus;
	protected static ActorRef orderEventBus;
	protected static ActorRef orderPlanningActor;
	
	private static final Logger logger = LoggerFactory.getLogger(OrderPlanningActorTest.class);
	
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		// setup shopfloor
		// setup machines
		// setup processes
		// setup order actors?
		// add processes to orderplanning actor
		system = ActorSystem.create(ROOT_SYSTEM);
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
	}

	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}
	
	@Test
	void testOrderRegisterWithoutAvailableMachines() {
		new TestKit(system) { 
			{ 
				String oid = "Order1";
				orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", oid)), getRef() );
				ProcessCore.Process p = TestMockMachineActor.getSequentialProcess();
				OrderProcess op = new OrderProcess(p);
				RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op, getRef());
				orderPlanningActor.tell(req, getRef());
				Boolean regOk = expectMsgPF(Duration.ofSeconds(1), "Register Order Event expected", event -> { 
					if (event instanceof OrderEvent) {
						return ((OrderEvent) event).getEventType().equals(OrderEventType.REGISTERED); 
					} else return false; });
				assert(regOk);
				expectMsgClass(OrderProcessUpdateEvent.class);
				Boolean pauseOk = expectMsgPF(Duration.ofSeconds(1), "Pause Order Event expected", event -> { 
					if (event instanceof OrderEvent) {
						return ((OrderEvent) event).getEventType().equals(OrderEventType.PAUSED); 
					} else return false; });
				assert(pauseOk);
			}	
		};
	}
	
	@Test
	void testOrderPlanningActorReceivingMachineActorCapabilities() {
		new TestKit(system) { 
			{ 
				String mid = "OrderMockMachine";
				machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", mid)), getRef() );
				ActorRef red1 = getMachineMockActor(1, "Red");
				ActorRef blue2 = getMachineMockActor(2, "Blue");
				ActorRef green3 = getMachineMockActor(3, "Green");
				ActorRef yellow4 = getMachineMockActor(4, "Yellow");
				//expectMsgClass(MachineUpdateEvent.class);
				//TODO: figure out how to inspect that capabilities are stored
			}	
		};
	}

	
	@Test
	void testOrderRegisterWithAlreadyAvailableMachines() {
		new TestKit(system) { 
			{ 
				String oid = "Order1";
				String mid = "OrderMockMachine";
				orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", oid)), getRef() );
				machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "*")), getRef() );
				//machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", mid)), getRef() );
				// if we subscribe to machinebus then we need to capture all the machine events by those actors
				ActorRef red1 = getMachineMockActor(1, "Red");
				expectMsgAnyClassOf(Duration.ofSeconds(3), MachineConnectedEvent.class);
				expectMsgAnyClassOf(Duration.ofSeconds(3), MachineUpdateEvent.class); //idle
				ActorRef blue2 = getMachineMockActor(2, "Blue");
				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
				ActorRef green3 = getMachineMockActor(3, "Green");
				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
				ActorRef yellow4 = getMachineMockActor(4, "Yellow");
				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
				expectMsgClass(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
				
				ProcessCore.Process p = TestMockMachineActor.getSequentialProcess();
				OrderProcess op = new OrderProcess(p);
				RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op, getRef());
				orderPlanningActor.tell(req, getRef());
				Boolean regOk = expectMsgPF(Duration.ofSeconds(3600), "Register Order Event expected", event -> { 
					if (event instanceof OrderEvent) {
						return ((OrderEvent) event).getEventType().equals(OrderEventType.REGISTERED); 
					} else return false; });
				assert(regOk);
				expectMsgClass(OrderProcessUpdateEvent.class); // First SubStep activated
				// then order requested at machine 
				Boolean pauseOk = expectMsgPF(Duration.ofSeconds(3600), "Pause Order Event expected", event -> { 
					if (event instanceof OrderEvent) {
						return ((OrderEvent) event).getEventType().equals(OrderEventType.SCHEDULED); 
					} else return false; });
				assert( pauseOk);
				for (int i=0; i < 27; i++) {
					System.out.println("i:"+i);
					TimedEvent te = expectMsgClass(Duration.ofSeconds(3600), TimedEvent.class);
					//logEvent(te);
				}
			}	
		};
	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}

	public static ActorRef getMachineMockActor(int id, String color) {
		ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		Actor modelActor = TestMockMachineActor.getDefaultMachineActor(id);
		AbstractCapability cap = TestMockMachineActor.composeInOne(TestMockMachineActor.getPlottingCapability(), TestMockMachineActor.getColorCapability(color));
		return system.actorOf(MockMachineActor.props(eventBusByRef, cap, modelActor));
	}
	
}
=======
package fiab.mes.order;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.function.Function;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockMachineActor;
import fiab.mes.mockactors.TestMockMachineActor;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.OrderPlanningActor;

class OrderPlanningActorTest {

	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "routes";
	protected static ActorRef machineEventBus;
	protected static ActorRef orderEventBus;
	protected static ActorRef orderPlanningActor;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		// setup shopfloor
		// setup machines
		// setup processes
		// setup order actors?
		// add processes to orderplanning actor
		system = ActorSystem.create(ROOT_SYSTEM);
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}
	
	@Test
	void testOrderRegisterWithoutAvailableMachines() {
		new TestKit(system) { 
			{ 
				String oid = "Order1";
				orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", oid)), getRef() );
				ProcessCore.Process p = TestMockMachineActor.getSequentialProcess();
				OrderProcess op = new OrderProcess(p);
				RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op, getRef());
				orderPlanningActor.tell(req, getRef());
				Boolean regOk = expectMsgPF(Duration.ofSeconds(1), "Register Order Event expected", event -> { 
					if (event instanceof OrderEvent) {
						return ((OrderEvent) event).getEventType().equals(OrderEventType.REGISTERED); 
					} else return false; });
				assert(regOk);
				expectMsgClass(OrderProcessUpdateEvent.class);
				Boolean pauseOk = expectMsgPF(Duration.ofSeconds(1), "Pause Order Event expected", event -> { 
					if (event instanceof OrderEvent) {
						return ((OrderEvent) event).getEventType().equals(OrderEventType.PAUSED); 
					} else return false; });
				assert(pauseOk);
			}	
		};
	}
	
	@Test
	void testOrderPlanningActorReceivingMachineActorCapabilities() {
		new TestKit(system) { 
			{ 
				String mid = "OrderMockMachine";
				machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", mid)), getRef() );
				ActorRef red1 = getMachineMockActor(1, "Red");
				ActorRef blue2 = getMachineMockActor(2, "Blue");
				ActorRef green3 = getMachineMockActor(3, "Green");
				ActorRef yellow4 = getMachineMockActor(4, "Yellow");
				//expectMsgClass(MachineUpdateEvent.class);
				//TODO: figure out how to inspect that capabilities are stored
			}	
		};
	}

	
	@Test
	void testOrderRegisterWithAlreadyAvailableMachines() {
		new TestKit(system) { 
			{ 
				String oid = "Order1";
				orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", oid)), getRef() );
				
				String mid = "OrderMockMachine";
				//machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", mid)), getRef() );
				// if we subscribe to machinebus then we need to capture all the machine events by those actors
				ActorRef red1 = getMachineMockActor(1, "Red");
				ActorRef blue2 = getMachineMockActor(2, "Blue");
				ActorRef green3 = getMachineMockActor(3, "Green");
				ActorRef yellow4 = getMachineMockActor(4, "Yellow");
				
				ProcessCore.Process p = TestMockMachineActor.getSequentialProcess();
				OrderProcess op = new OrderProcess(p);
				RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op, getRef());
				orderPlanningActor.tell(req, getRef());
				Boolean regOk = expectMsgPF(Duration.ofSeconds(3600), "Register Order Event expected", event -> { 
					if (event instanceof OrderEvent) {
						return ((OrderEvent) event).getEventType().equals(OrderEventType.REGISTERED); 
					} else return false; });
				assert(regOk);
				expectMsgClass(OrderProcessUpdateEvent.class); // First SubStep activated
				// then order requested at machine and paused until response received
				Boolean pauseOk = expectMsgPF(Duration.ofSeconds(3600), "Pause Order Event expected", event -> { 
					if (event instanceof OrderEvent) {
						return ((OrderEvent) event).getEventType().equals(OrderEventType.PAUSED); 
					} else return false; });
				assert(pauseOk);
				
			}	
		};
	}

	public static ActorRef getMachineMockActor(int id, String color) {
		ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		Actor modelActor = TestMockMachineActor.getDefaultMachineActor(id);
		AbstractCapability cap = TestMockMachineActor.composeInOne(TestMockMachineActor.getPlottingCapability(), TestMockMachineActor.getColorCapability(color));
		return system.actorOf(MockMachineActor.props(eventBusByRef, cap, modelActor));
	}
	
}
>>>>>>> parent of c7005b3... finished first test of order processing by 4 machines via the orderplanningactor
