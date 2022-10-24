package fiab.mes.order;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.shopfloor.layout.DefaultTestLayout;
import fiab.mes.shopfloor.layout.ShopfloorLayout;
import fiab.mes.shopfloor.utils.TransportPositionLookupAndParser;
import fiab.mes.shopfloor.utils.TransportRoutingAndMappingInterface;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.CancelOrTerminateOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;

@Tag("IntegrationTest")
class OrderCancelTest {

	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "routes";	
	protected static ActorRef orderEventBus;
	protected static ActorRef orderPlanningActor;
	protected static ActorRef coordActor;
	//protected static DefaultLayout layout;
	private ShopfloorLayout layout;

	private static final Logger logger = LoggerFactory.getLogger(OrderCancelTest.class);
	static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
	
	@BeforeEach
	public void setup() throws Exception {
		// setup shopfloor
		// setup machines
		// setup processes
		// setup order actors?
		// add processes to orderplanning actor		
		system = ActorSystem.create(ROOT_SYSTEM);
		ActorRef interMachineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		layout = new DefaultTestLayout(system, interMachineEventBus);
		TransportRoutingAndMappingInterface routing = layout.getTransportRoutingAndMapping();
		TransportPositionLookupAndParser dns = layout.getTransportPositionLookup();
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 2), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
		orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
		knownActors.clear();
		
	}

	@AfterEach
	public void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test //FIXME: machine of order 1 is suddenly not found anymore
	void testCancelBeforeAssignment() {
		new TestKit(system) { 
			{
				layout.subscribeToInterMachineEventBus(getRef(), "Tester");
				layout.initializeAndDiscoverParticipants(getRef());
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
				String oid3 = "Order3";
				subscribeAndRegisterSinglePrintRedOrder(oid1, getRef());
				subscribeAndRegisterSinglePrintRedOrder(oid2, getRef());
				subscribeAndRegisterSinglePrintRedOrder(oid3, getRef());
				boolean order1Done = false;
				boolean order2Done = false;
				boolean order3Done = false;
				while (!order1Done || !order2Done || !order3Done) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class);
					logEvent(te);
					if (te instanceof OrderEvent) {
						OrderEvent oe = (OrderEvent) te;
						if (matches(oe, oid2, OrderEventType.PAUSED)) {
							cancelOrder(oid2, getRef()); // we cancel when not yet assigned = first PAUSED
						}
						if (oe.getEventType().equals(OrderEvent.OrderEventType.COMPLETED)) {
							System.out.println(" ---------------- Order complete: "+oe.getOrderId());
						}	
						if (matches(oe, oid1, OrderEventType.REMOVED))
							order1Done = true;
						if (matches(oe, oid3, OrderEventType.REMOVED))
							order3Done = true;
						if (matches(oe, oid2, OrderEventType.REJECTED))
							order2Done = true;
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
	
	@Test
	void testCancelBeforeReqTransport() {
		new TestKit(system) { 
			{
				layout.subscribeToInterMachineEventBus(getRef(), "Tester");
				layout.initializeAndDiscoverParticipants(getRef());
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
				OrderProcess p1 = subscribeAndRegisterPrintGreenAndRedOrder(oid1, getRef());
				subscribeAndRegisterSinglePrintRedOrder(oid2, getRef());
				boolean order1Done = false;
				while (!order1Done ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), TimedEvent.class);
					logEvent(te);
					if (te instanceof OrderEvent) {
						OrderEvent oe = (OrderEvent) te;
						if (matches(oe, oid1, OrderEventType.TRANSPORT_IN_PROGRESS)) {
							cancelOrder(oid1, getRef()); // we cancel when not yet assigned = first PAUSED
						}
						if (oe.getEventType().equals(OrderEvent.OrderEventType.COMPLETED)) {
							System.out.println(" ---------------- Order complete: "+oe.getOrderId());
						}	
						if (matches(oe, oid1, OrderEventType.PREMATURE_REMOVAL)) {
							order1Done = true;
							assert(p1.stepStatus.get(p1.getProcess().getSteps().get(1)).equals(StepStatusEnum.CANCELED));
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
	
	@Test
	void testCancelWhileExecute()  {
		new TestKit(system) { 
			{
				layout.subscribeToInterMachineEventBus(getRef(), "Tester");
				layout.initializeAndDiscoverParticipants(getRef());
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
				OrderProcess p1 = subscribeAndRegisterPrintGreenAndRedOrder(oid1, getRef());
				subscribeAndRegisterSinglePrintRedOrder(oid2, getRef());
				boolean order1Done = false;
				boolean order2Done = false;
				
				while (!order1Done || !order2Done ) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), TimedEvent.class);
					logEvent(te);
					if (te instanceof OrderEvent) {
						OrderEvent oe = (OrderEvent) te;
						if (matches(oe, oid1, OrderEventType.PRODUCING)) {
							cancelOrder(oid1, getRef()); // we cancel when not yet assigned = first PAUSED
						}
						if (oe.getEventType().equals(OrderEvent.OrderEventType.COMPLETED)) {
							System.out.println(" ---------------- Order complete: "+oe.getOrderId());
						}	
						if (matches(oe, oid2, OrderEventType.REMOVED))
							order2Done = true;
						if (matches(oe, oid1, OrderEventType.REMOVED)) {
							order1Done = true;
							assert(p1.stepStatus.get(p1.getProcess().getSteps().get(1)).equals(StepStatusEnum.CANCELED));
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
	
		
	public void cancelOrder(String oid, ActorRef testProbe) {
		CancelOrTerminateOrder req = new CancelOrTerminateOrder(oid);
		orderPlanningActor.tell(req, testProbe);
	}
	
	public OrderProcess subscribeAndRegisterSinglePrintRedOrder(String oid, ActorRef testProbe) {		
		orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe );
		OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleRedStepProcess(oid));				
		RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
		orderPlanningActor.tell(req, testProbe);
		return op1;
	}
	
	public void subscribeAndRegisterSinglePrintGreenOrder(String oid, ActorRef testProbe) {		
		orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe );
		OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleGreenStepProcess(oid));				
		RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
		orderPlanningActor.tell(req, testProbe);
	}
	
	public OrderProcess subscribeAndRegisterPrintGreenAndRedOrder(String oid, ActorRef testProbe) {		
		orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe );
		OrderProcess op1 = new OrderProcess(ProduceProcess.getRedAndGreenStepProcess(oid));				
		RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
		orderPlanningActor.tell(req, testProbe);
		return op1;
	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}
	
	private boolean matches(OrderEvent e, String orderId, OrderEventType type) {
		return (e.getEventType().equals(type) && e.getOrderId().equals(orderId));
	}
}
