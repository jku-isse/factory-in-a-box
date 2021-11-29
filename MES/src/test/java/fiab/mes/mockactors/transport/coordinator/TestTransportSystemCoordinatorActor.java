package fiab.mes.mockactors.transport.coordinator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.shopfloor.DefaultLayout;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;

class TestTransportSystemCoordinatorActor {

	private static final Logger logger = LoggerFactory.getLogger(TestTransportSystemCoordinatorActor.class);
	
	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "TEST_TRANSPORTSYSTEM";
	protected static ActorRef machineEventBus;
	protected static ActorRef orderEventBus;
	protected static ActorRef coordActor;
	HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
	DefaultTransportPositionLookup dns = new DefaultTransportPositionLookup();
//	static VirtualIOStationActorFactory partsIn;
//	static VirtualIOStationActorFactory partsOut;
	static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
	static DefaultLayout layout;
//	private static boolean engageAutoReload = true;
//	private static boolean disengageAutoReload = false;
	
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		// setup shopfloor
		// setup machines				
		system = ActorSystem.create(ROOT_SYSTEM);
		layout = new DefaultLayout(system);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		
	}

	@BeforeEach
	public static void setupBeforeEach() {
		knownActors.clear();
	}
	
	@AfterAll
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	
	@Test //WORKS
	void testCoordinatorWithSingleTTLayout() throws Exception {
		new TestKit(system) { 
			{ 				
				layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), "TransportCoordinator");				
				//setupTwoTurntableWithIOShopfloor();
				layout.setupSingleTT21withIOEast35West20();
				int countConnEvents = 0;
				while (countConnEvents < 3) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent)
						countConnEvents++;					
				}
	//			assert(dns.getActorForPosition(new Position("21")).get().equals(knownActors.get("MockTurntableActor21")));
				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("InputStationActor20"), knownActors.get("OutputStationActor35"), "TestOrder1", getRef());
				coordActor.tell(rtr, getRef());
				
				boolean transportDone = false;
				boolean resetTT = false;
				while(!transportDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent && ((MachineStatusUpdateEvent) te).getMachineId().equals("MockTurntableActor21") && !resetTT) {
						knownActors.get("MockTurntableActor21").getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineEvent) te).getMachineId()), getRef());
						resetTT = true;
					}
					if (te instanceof RegisterTransportRequestStatusResponse) {
						RegisterTransportRequestStatusResponse rtrr = (RegisterTransportRequestStatusResponse) te;
						if (rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.COMPLETED)) {
							transportDone = true;
						} else { 
							assertTrue(rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.QUEUED) ||
									rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.ISSUED)) ;
						}
					}
				}
			}	
		};
	}
	
	@Test //WORKS
	void testCoordinatorWithDualTTLayout() throws Exception {
		new TestKit(system) { 
			{ 				
				layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), "TransportCoordinator");				
				//setupTwoTurntableWithIOShopfloor();
				layout.setupDualTT2021withIOEast35West34();
				int countConnEvents = 0;
				while (countConnEvents < 4) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent)
						countConnEvents++;					
				}
	//			assert(dns.getActorForPosition(new Position("21")).get().equals(knownActors.get("MockTurntableActor21")));
				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("InputStationActor34"), knownActors.get("OutputStationActor35"), "TestOrder1", getRef());
				coordActor.tell(rtr, getRef());
				
				boolean transportDone = false;
				boolean resetTT1 = false;
				boolean resetTT2 = false;
				while(!transportDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent && ((MachineStatusUpdateEvent) te).getMachineId().equals("MockTurntableActor20") && !resetTT1) {
						knownActors.get("MockTurntableActor20").getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineEvent) te).getMachineId()), getRef());
						resetTT1 = true;
					}
					if (te instanceof MachineStatusUpdateEvent && ((MachineStatusUpdateEvent) te).getMachineId().equals("MockTurntableActor21") && !resetTT2) {
						knownActors.get("MockTurntableActor21").getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineEvent) te).getMachineId()), getRef());
						resetTT2 = true;
					}
					if (te instanceof RegisterTransportRequestStatusResponse) {
						RegisterTransportRequestStatusResponse rtrr = (RegisterTransportRequestStatusResponse) te;
						if (rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.COMPLETED)) {
							transportDone = true;
						} else { 
							assertTrue(rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.QUEUED) ||
									rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.ISSUED)) ;
						}
					}
				}
			}	
		};
	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
		if (event instanceof MachineConnectedEvent) {
			knownActors.put(((MachineConnectedEvent) event).getMachineId(), ((MachineConnectedEvent) event).getMachine());
		}
	}
	

	
}
