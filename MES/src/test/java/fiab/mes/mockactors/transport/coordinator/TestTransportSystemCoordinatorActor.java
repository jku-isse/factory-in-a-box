package fiab.mes.mockactors.transport.coordinator;

import static fiab.mes.shopfloor.utils.ShopfloorUtils.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;

import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.shopfloor.layout.DefaultTestLayout;
import fiab.mes.shopfloor.layout.ShopfloorLayout;
import fiab.mes.shopfloor.layout.SingleTurntableLayout;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import org.junit.jupiter.api.*;
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
	//HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
	//DefaultTransportPositionLookup dns = new DefaultTransportPositionLookup();
//	static VirtualIOStationActorFactory partsIn;
//	static VirtualIOStationActorFactory partsOut;
	static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
	static ShopfloorLayout layout;
//	private static boolean engageAutoReload = true;
//	private static boolean disengageAutoReload = false;
	
	@BeforeEach
	public void setup() throws Exception {
		// setup shopfloor
		// setup machines				
		system = ActorSystem.create(ROOT_SYSTEM);
		//layout = new DefaultLayout(system);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	}
	
	@AfterEach
	public void teardown() {
		knownActors.clear();
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test
	@Tag("IntegrationTest")
	void testCoordinatorWithSingleTTLayout() throws Exception {
		new TestKit(system) { 
			{
				layout = new SingleTurntableLayout(system, machineEventBus);
				layout.subscribeToInterMachineEventBus(getRef(), "Tester");
				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(layout.getTransportRoutingAndMapping(), layout.getTransportPositionLookup(), 1), "TransportCoordinator");
				layout.initializeAndDiscoverParticipants(getRef());
				int countConnEvents = 0;
				boolean transportReady = false;
				while (countConnEvents < layout.getParticipants().size() && !transportReady) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++;
						MachineConnectedEvent connectedEvent = ((MachineConnectedEvent) te);
						knownActors.put(connectedEvent.getMachineId(), connectedEvent.getMachine());
					}
					if(te instanceof TransportSystemStatusMessage){
						if(((TransportSystemStatusMessage) te).getState() == TransportSystemStatusMessage.State.FULLY_OPERATIONAL){
							transportReady = true;
						}
					}
				}
				String inputStationId = layout.getParticipantForId(INPUT_STATION).getProxyMachineId();
				String outputStationId = layout.getParticipantForId(OUTPUT_STATION).getProxyMachineId();
				String turntableId = layout.getParticipantForId(TURNTABLE_1).getProxyMachineId();
				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get(inputStationId), knownActors.get(outputStationId), "TestOrder1", getRef());
				coordActor.tell(rtr, getRef());
				
				boolean transportDone = false;
				boolean resetTT = false;
				while(!transportDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class, TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent && ((MachineStatusUpdateEvent) te).getMachineId().equals(turntableId) && !resetTT) {
						knownActors.get(turntableId).getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineEvent) te).getMachineId()), getRef());
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
	
	@Test	//FIXME only passes when test run individually
	@Tag("IntegrationTest")
	void testCoordinatorWithDualTTLayout() {
		new TestKit(system) { 
			{
				layout = new DefaultTestLayout(system, machineEventBus);
				layout.subscribeToInterMachineEventBus(getRef(), "Tester");
				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(layout.getTransportRoutingAndMapping(), layout.getTransportPositionLookup(), 1), "TransportCoordinator");
				layout.initializeAndDiscoverParticipants(getRef());
				int countConnEvents = 0;
				while (countConnEvents < layout.getParticipants().size()) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof MachineConnectedEvent)
						countConnEvents++;					
				}
				String inputStationId = layout.getParticipantForId(INPUT_STATION).getProxyMachineId();
				String outputStationId = layout.getParticipantForId(OUTPUT_STATION).getProxyMachineId();
				String turntableId = layout.getParticipantForId(TURNTABLE_1).getProxyMachineId();
				String turntable2Id = layout.getParticipantForId(TURNTABLE_2).getProxyMachineId();
				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get(inputStationId), knownActors.get(outputStationId), "TestOrder1", getRef());
				coordActor.tell(rtr, getRef());
				
				boolean transportDone = false;
				boolean resetTT1 = false;
				boolean resetTT2 = false;
				while(!transportDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent && ((MachineStatusUpdateEvent) te).getMachineId().equals(turntableId) && !resetTT1) {
						knownActors.get(turntableId).getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineEvent) te).getMachineId()), getRef());
						resetTT1 = true;
					}
					if (te instanceof MachineStatusUpdateEvent && ((MachineStatusUpdateEvent) te).getMachineId().equals(turntable2Id) && !resetTT2) {
						knownActors.get(turntable2Id).getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineEvent) te).getMachineId()), getRef());
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
