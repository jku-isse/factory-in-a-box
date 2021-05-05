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
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import brave.Span;
import fiab.core.capabilities.basicmachine.events.MachineEvent;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.tracing.TestTracingUtil;
import fiab.handshake.actor.ClientHandshakeActor;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.ServerSideHandshakeActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.shopfloor.DefaultLayout;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.tracing.impl.zipkin.ZipkinUtil;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.actor.TransportModuleCoordinatorActor;
import fiab.turntable.conveying.BaseBehaviorConveyorActor;
import fiab.turntable.turning.BaseBehaviorTurntableActor;

class TestTransportSystemCoordinatorActor {

	private static final Logger logger = LoggerFactory.getLogger(TestTransportSystemCoordinatorActor.class);

	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "TEST_TRANSPORTSYSTEM";
	protected static ActorRef machineEventBus;
	protected static ActorRef orderEventBus;
	protected static ActorRef coordActor;
	HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
	TransportPositionLookup dns = new TransportPositionLookup();
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
		system.registerExtension(TestTracingUtil.getTracingExtension());
		layout = new DefaultLayout(system);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(),
				OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	}

	@BeforeEach
	public void setupBeforeEach() {
		knownActors.clear();
	}

	@AfterAll
	public static void teardown() {
		TestKit.shutdownActorSystem(system);
		system = null;
	}

	@Test // WORKS
	void testCoordinatorWithSingleTTLayout() throws Exception {
		new TestKit(system) {
			{
				layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")),
						getRef());
				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1),
						"TransportCoordinator");
				// setupTwoTurntableWithIOShopfloor();
				layout.setupSingleTT21withIOEast35West20();
				int countConnEvents = 0;
				while (countConnEvents < 3) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), IOStationStatusUpdateEvent.class,
							MachineStatusUpdateEvent.class, MachineConnectedEvent.class,
							TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof MachineConnectedEvent)
						countConnEvents++;
				}
				// assert(dns.getActorForPosition(new
				// Position("21")).get().equals(knownActors.get("MockTurntableActor21")));
				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("InputStationActor20"),
						knownActors.get("OutputStationActor35"), "TestOrder1", getRef());
				coordActor.tell(rtr, getRef());

				boolean transportDone = false;
				boolean resetTT = false;
				while (!transportDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class,
							IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class,
							MachineConnectedEvent.class, TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent
							&& ((MachineStatusUpdateEvent) te).getMachineId().equals("MockTurntableActor21")
							&& !resetTT) {
						knownActors.get("MockTurntableActor21").getAkkaActor()
								.tell(new GenericMachineRequests.Reset(((MachineEvent) te).getMachineId()), getRef());
						resetTT = true;
					}
					if (te instanceof RegisterTransportRequestStatusResponse) {
						RegisterTransportRequestStatusResponse rtrr = (RegisterTransportRequestStatusResponse) te;
						if (rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.COMPLETED)) {
							transportDone = true;
						} else {
							assertTrue(rtrr.getResponse()
									.equals(RegisterTransportRequestStatusResponse.ResponseType.QUEUED)
									|| rtrr.getResponse()
											.equals(RegisterTransportRequestStatusResponse.ResponseType.ISSUED));
						}
					}
				}
			}
		};
	}

	@Test // WORKS
	void testCoordinatorWithDualTTLayout() throws Exception {
		new TestKit(system) {
			{
				layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")),
						getRef());
				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1),
						"TransportCoordinator");
				// setupTwoTurntableWithIOShopfloor();
				layout.setupDualTT2021withIOEast35West34();
				int countConnEvents = 0;
				while (countConnEvents < 4) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class,
							IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class,
							TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof MachineConnectedEvent)
						countConnEvents++;
				}
				// assert(dns.getActorForPosition(new
				// Position("21")).get().equals(knownActors.get("MockTurntableActor21")));
				Span span = TestTracingUtil.createNewRandomSpan().name("Parent Span").start();

				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("InputStationActor34"),
						knownActors.get("OutputStationActor35"), "TestOrder1", getRef());
				rtr.setTracingHeader(ZipkinUtil.createXB3Header(span));
				TestTracingUtil.getInjector().inject(span.context(), rtr);

				coordActor.tell(rtr, getRef());

				boolean transportDone = false;
				boolean resetTT1 = false;
				boolean resetTT2 = false;
				while (!transportDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class,
							IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class,
							TransportSystemStatusMessage.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent
							&& ((MachineStatusUpdateEvent) te).getMachineId().equals("MockTurntableActor20")
							&& !resetTT1) {
						knownActors.get("MockTurntableActor20").getAkkaActor()
								.tell(new GenericMachineRequests.Reset(((MachineEvent) te).getMachineId()), getRef());
						resetTT1 = true;
					}
					if (te instanceof MachineStatusUpdateEvent
							&& ((MachineStatusUpdateEvent) te).getMachineId().equals("MockTurntableActor21")
							&& !resetTT2) {
						knownActors.get("MockTurntableActor21").getAkkaActor()
								.tell(new GenericMachineRequests.Reset(((MachineEvent) te).getMachineId()), getRef());
						resetTT2 = true;
					}
					if (te instanceof RegisterTransportRequestStatusResponse) {
						RegisterTransportRequestStatusResponse rtrr = (RegisterTransportRequestStatusResponse) te;
						if (rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.COMPLETED)) {
							transportDone = true;
						} else {
							assertTrue(rtrr.getResponse()
									.equals(RegisterTransportRequestStatusResponse.ResponseType.QUEUED)
									|| rtrr.getResponse()
											.equals(RegisterTransportRequestStatusResponse.ResponseType.ISSUED));
						}
					}
				}				
				span.finish();
				System.out.println("Finished with emitting orders. Press ENTER to end test!");
				System.in.read();
				System.out.println("Test completed");
				
				TestTracingUtil.finishSpan();
			}
		};
	}

	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
		if (event instanceof MachineConnectedEvent) {
			knownActors.put(((MachineConnectedEvent) event).getMachineId(),
					((MachineConnectedEvent) event).getMachine());
		}
	}

}
