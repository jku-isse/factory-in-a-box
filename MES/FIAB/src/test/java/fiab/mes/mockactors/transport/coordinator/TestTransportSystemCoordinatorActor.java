package fiab.mes.mockactors.transport.coordinator;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.MockClientHandshakeActor;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.mockactors.iostation.MockIOStationFactory;
import fiab.mes.mockactors.oldplotter.TestMockMachineActor;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper;
import fiab.mes.mockactors.transport.MockTransportModuleWrapperDelegate;
import fiab.mes.mockactors.transport.TestMockTransportModuleActor;
import fiab.mes.mockactors.transport.LocalEndpointStatus;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.order.msg.OrderEvent.OrderEventType;

class TestTransportSystemCoordinatorActor {

	private static final Logger logger = LoggerFactory.getLogger(TestTransportSystemCoordinatorActor.class);
	
	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "routes";
	protected static ActorRef machineEventBus;
	protected static ActorRef orderEventBus;
	protected static ActorRef coordActor;
	HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
	TransportPositionLookup dns = new TransportPositionLookup();
	static MockIOStationFactory partsIn;
	static MockIOStationFactory partsOut;
	static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
	
	private static boolean engageAutoReload = true;
	private static boolean disengageAutoReload = false;
	
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		// setup shopfloor
		// setup machines				
		system = ActorSystem.create(ROOT_SYSTEM);
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		
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
	
	@Test
	void testCoordinatorWith2TTLayout() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{ 															
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );

				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns), "TransportCoordinator");				
				setupTwoTurntableWithIOShopfloor();
				int countConnEvents = 0;
				while (countConnEvents < 4) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent)
						countConnEvents++;					
				}
				assert(dns.getActorForPosition(new Position("20")).get().equals(knownActors.get("MockTurntableActor20")));
				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("InputStationActor34"), knownActors.get("OutputStationActor35"), "TestOrder1", getRef());
				coordActor.tell(rtr, getRef());
				
				boolean transportDone = false;
				while(!transportDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class);
					logEvent(te);
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
	
	private void setupTwoTurntableWithIOShopfloor() throws InterruptedException, ExecutionException {
		final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		
		partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload, 34);
		partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload, 35);
		// now add to ttWrapper client Handshake actors
		ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
		ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
		ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
	
		// setup turntable1
		InterMachineEventBus intraEventBus1 = new InterMachineEventBus();	
		ActorRef ttWrapper1 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus1), "TT1");
		ActorRef westClient1 = system.actorOf(MockClientHandshakeActor.props(ttWrapper1, inRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT);
		boolean autoComplete = true;
		ActorRef eastServer1 = system.actorOf(MockServerHandshakeActor.props(ttWrapper1, autoComplete), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER);
		ttWrapper1.tell( new LocalEndpointStatus.LocalClientEndpointStatus(westClient1, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT), ActorRef.noSender());
		ttWrapper1.tell( new LocalEndpointStatus.LocalServerEndpointStatus(eastServer1, true, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER), ActorRef.noSender());
		ttWrapper1.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
		ttWrapper1.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
		
		
		// setup turntable 2
		InterMachineEventBus intraEventBus2 = new InterMachineEventBus();	
		ActorRef ttWrapper2 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus2), "TT2");
		ActorRef eastClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, outRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT);
		ActorRef westClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, eastServer1), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT+"~2");		
		ttWrapper2.tell( new LocalEndpointStatus.LocalClientEndpointStatus(westClient2, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT), ActorRef.noSender());
		ttWrapper2.tell( new LocalEndpointStatus.LocalClientEndpointStatus(eastClient2, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT), ActorRef.noSender());
		ttWrapper2.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
		ttWrapper2.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
		
		// setup actual turntable actors:
		setupTurntableActor(ttWrapper1, intraEventBus1, eventBusByRef, 20);
		setupTurntableActor(ttWrapper2, intraEventBus2, eventBusByRef, 21);
	}
	
//	@Test - TODO: needs fixing
//	void testCoordinatorWithMinimumLayout() throws InterruptedException, ExecutionException {
//		new TestKit(system) { 
//			{ 															
//				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
//				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
//
//				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns), "TransportCoordinator");				
//				setupSingleTurntableWithIOShopfloor();
//				for (int i=0; i<11; i++) {
//					logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class));
//				}
//				assert(dns.getActorForPosition(new Position("21")).get().equals(knownActors.get("MockTurntableActor21")));
//				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("InputStationActor20"), knownActors.get("OutputStationActor35"), "TestOrder1", getRef());
//				coordActor.tell(rtr, getRef());
//				
//				for (int i=0; i<30; i++) {
//					logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class));
//				}
//			}	
//		};
//	}
	
	private void setupSingleTurntableWithIOShopfloor() throws InterruptedException, ExecutionException {
		final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		
		partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload, 20);
		partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload, 35);
		// now add to ttWrapper client Handshake actors
		ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
		ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
		ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();			
		
		// setup turntable 2
		InterMachineEventBus intraEventBus2 = new InterMachineEventBus();	
		ActorRef ttWrapper2 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus2), "TT2");
		ActorRef eastClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, outRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT);
		ActorRef westClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, inRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT+"~2");
		ttWrapper2.tell( new LocalEndpointStatus.LocalClientEndpointStatus(westClient2, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT), ActorRef.noSender());
		ttWrapper2.tell( new LocalEndpointStatus.LocalClientEndpointStatus(eastClient2, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT), ActorRef.noSender());
		ttWrapper2.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
		ttWrapper2.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
		
		// setup actual turntable actors:
		setupTurntableActor(ttWrapper2, intraEventBus2, eventBusByRef, 21);
	}
	
	
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
		if (event instanceof MachineConnectedEvent) {
			knownActors.put(((MachineConnectedEvent) event).getMachineId(), ((MachineConnectedEvent) event).getMachine());
		}
	}
	
	private ActorRef setupTurntableActor(ActorRef ttWrapper, InterMachineEventBus intraEventBus, ActorSelection eventBusByRef, int ipid ) {
		AbstractCapability cap = TransportModuleCapability.getTransportCapability();
		Actor modelActor = TestMockTransportModuleActor.getDefaultTransportModuleModelActor(ipid);
		MockTransportModuleWrapperDelegate hal = new MockTransportModuleWrapperDelegate(ttWrapper);
		Position selfPos = new Position(ipid+"");
		HardcodedDefaultTransportRoutingAndMapping env = new HardcodedDefaultTransportRoutingAndMapping();								
		return system.actorOf(BasicTransportModuleActor.props(eventBusByRef, cap, modelActor, hal, selfPos, intraEventBus, new TransportPositionLookup(), env), "TTActor"+ipid);
	}
	
}
