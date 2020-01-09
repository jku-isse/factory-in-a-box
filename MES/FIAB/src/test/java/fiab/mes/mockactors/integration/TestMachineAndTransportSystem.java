package fiab.mes.mockactors.integration;

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
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.MockClientHandshakeActor;
import fiab.mes.mockactors.MockPlottingMachineWrapperDelegate;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.mockactors.MockTransportAwareMachineWrapper;
import fiab.mes.mockactors.TestBasicMachineActorWithTransport;
import fiab.mes.mockactors.TestMockMachineActor;
import fiab.mes.mockactors.MockServerHandshakeActor.MessageTypes;
import fiab.mes.mockactors.iostation.MockIOStationFactory;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper;
import fiab.mes.mockactors.transport.MockTransportModuleWrapperDelegate;
import fiab.mes.mockactors.transport.TestMockTransportModuleActor;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper.LocalEndpointStatus;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.order.msg.OrderEvent.OrderEventType;

class TestMachineAndTransportSystem {

	private static final Logger logger = LoggerFactory.getLogger(TestMachineAndTransportSystem.class);
	
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
	void testTransport34InTo31Mto35Out() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{ 															
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );

				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns), "TransportCoordinator");				
				setupTwoTurntableWith2MachinesAndIO();
				boolean machineReady = false;
				int countConnEvents = 0;
				while (countConnEvents < 8) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
						//System.out.println("Connected Event: "+countConnEvents);
					}
					if (te instanceof MachineStatusUpdateEvent && !machineReady) {
						machineReady = lockMachineforRequest((MachineStatusUpdateEvent) te, "MockMachineActor31", getRef());
					}
				}
				assert(dns.getActorForPosition(new Position("20")).get().equals(knownActors.get("MockTurntableActor20")));
				// now bring one machine into ready state if not already done
				while (!machineReady) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent) {
						machineReady = lockMachineforRequest((MachineStatusUpdateEvent) te, "MockMachineActor31", getRef());
					}
				}
				// once ready and assigned, then trigger transport request
				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("InputStationActor34"), knownActors.get("MockMachineActor31"), "TestOrder1", getRef());
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
//				// then unload machine once machine is done
				boolean machineDone = false;
				transportDone = false;
				while (!machineDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent) {
						machineDone = unloadMachineforRequest((MachineStatusUpdateEvent) te, "MockMachineActor31", getRef());
					} 
				}
				// and we wait for unloading to complete
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
	
	private void setupTwoTurntableWith2MachinesAndIO() throws InterruptedException, ExecutionException {
		final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		
		partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload, 34);
		partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload, 35);
		// now add to ttWrapper client Handshake actors
		ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
		ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
		ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
	
		// Machines for first turnable
		ActorRef handShakeServer31 = setupMachineActor(eventBusByRef, 31, TestBasicMachineActorWithTransport.getColorCapability("Red"));
		ActorRef handShakeServer37 = setupMachineActor(eventBusByRef, 37, TestBasicMachineActorWithTransport.getColorCapability("Blue"));				
		// setup turntable1
		InterMachineEventBus intraEventBus1 = new InterMachineEventBus();	
		ActorRef ttWrapper1 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus1), "TT1");
		ActorRef westClient1 = system.actorOf(MockClientHandshakeActor.props(ttWrapper1, inRef), WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT);
		ActorRef northClient1 = system.actorOf(MockClientHandshakeActor.props(ttWrapper1, handShakeServer31), WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT);
		ActorRef southClient1 = system.actorOf(MockClientHandshakeActor.props(ttWrapper1, handShakeServer37), WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT);
		boolean autoComplete = true;
		ActorRef eastServer1 = system.actorOf(MockServerHandshakeActor.props(ttWrapper1, autoComplete), WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_SERVER);
		Map<String, LocalEndpointStatus> eps1 = new HashMap<>();

		eps1.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(northClient1, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT));
		eps1.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(southClient1, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT));
		eps1.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient1, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT));
		eps1.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_SERVER, new MockTransportModuleWrapper.LocalServerEndpointStatus(eastServer1, true, WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_SERVER));
		ttWrapper1.tell(new MockTransportModuleWrapper.HandshakeEndpointInfo(eps1), ActorRef.noSender());
		ttWrapper1.tell(MockTransportModuleWrapper.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
		ttWrapper1.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, ActorRef.noSender());
		
		// Machines for second turntable
		ActorRef handShakeServer32 = setupMachineActor(eventBusByRef, 32, TestBasicMachineActorWithTransport.getColorCapability("Yellow"));
		ActorRef handShakeServer38 = setupMachineActor(eventBusByRef, 38, TestBasicMachineActorWithTransport.getColorCapability("Green"));
		// setup turntable 2
		InterMachineEventBus intraEventBus2 = new InterMachineEventBus();	
		ActorRef ttWrapper2 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus2), "TT2");
		ActorRef eastClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, outRef), WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT);
		ActorRef westClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, eastServer1), WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT+"~2");
		ActorRef northClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, handShakeServer32), WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT+"~2");
		ActorRef southClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, handShakeServer38), WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT+"~2");
		
		Map<String, LocalEndpointStatus> eps2 = new HashMap<>();
		eps2.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(northClient2, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT));
		eps2.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(southClient2, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT));		
		eps2.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient2, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT));
		eps2.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(eastClient2, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT));
		ttWrapper2.tell(new MockTransportModuleWrapper.HandshakeEndpointInfo(eps2), ActorRef.noSender());
		ttWrapper2.tell(MockTransportModuleWrapper.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
		ttWrapper2.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, ActorRef.noSender());
		
		// setup actual turntable actors:
		setupTurntableActor(ttWrapper1, intraEventBus1, eventBusByRef, 20);
		setupTurntableActor(ttWrapper2, intraEventBus2, eventBusByRef, 21);
	}
	
	
	private boolean lockMachineforRequest(MachineStatusUpdateEvent mue, String matchMachineId, ActorRef testSys) {
		MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
		if (newState.equals(MachineStatus.IDLE) && mue.getMachineId().equals(matchMachineId)) {
			ActorRef machine = knownActors.get(matchMachineId).getAkkaActor();
			logger.info("Sending lock for Order request to machine");
			machine.tell(new LockForOrder("TestStep1","TestRootOrder1"), testSys); // here we dont register and wait for readyness, wont work later
			return true;
		} 
		return false;
	}
	
	private boolean unloadMachineforRequest(MachineStatusUpdateEvent mue, String matchMachineId, ActorRef testSys) {
		MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
		if (newState.equals(MachineStatus.COMPLETING) && mue.getMachineId().equals(matchMachineId)) {
			RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get(matchMachineId), knownActors.get("OutputStationActor35"), "TestOrder2", testSys);
			coordActor.tell(rtr, testSys);
			return true;
		} 
		return false;
	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
		if (event instanceof MachineConnectedEvent) {
			knownActors.put(((MachineConnectedEvent) event).getMachineId(), ((MachineConnectedEvent) event).getMachine());
		}
	}
	
	private ActorRef setupTurntableActor(ActorRef ttWrapper, InterMachineEventBus intraEventBus, ActorSelection eventBusByRef, int ipid ) {
		AbstractCapability cap = WellknownTransportModuleCapability.getTurntableCapability();
		Actor modelActor = TestMockTransportModuleActor.getDefaultTransportModuleModelActor(ipid);
		MockTransportModuleWrapperDelegate hal = new MockTransportModuleWrapperDelegate(ttWrapper);
		Position selfPos = new Position(ipid+"");
		HardcodedDefaultTransportRoutingAndMapping env = new HardcodedDefaultTransportRoutingAndMapping();								
		return system.actorOf(BasicTransportModuleActor.props(eventBusByRef, cap, modelActor, hal, selfPos, intraEventBus, new TransportPositionLookup(), env), "TTActor"+ipid);
	}
	
	private ActorRef setupMachineActor(ActorSelection eventBusByRef, int ipid, AbstractCapability colorCap) throws InterruptedException, ExecutionException {
		InterMachineEventBus intraEventBus = new InterMachineEventBus();
		final AbstractCapability cap = TestBasicMachineActorWithTransport.composeInOne(TestBasicMachineActorWithTransport.getPlottingCapability(), colorCap);
		final Actor modelActor = TestBasicMachineActorWithTransport.getDefaultMachineActor(ipid);
		ActorRef machineWrapper = system.actorOf(MockTransportAwareMachineWrapper.props(intraEventBus), "MachineWrapper"+ipid);
		ActorSelection serverSide = system.actorSelection("/user/MachineWrapper"+ipid+"/ServerSideHandshakeMock");
		Thread.sleep(1000);
		ActorRef serverSideRef = serverSide.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
		ActorRef machine = system.actorOf(BasicMachineActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
		return serverSideRef;
	}
}
