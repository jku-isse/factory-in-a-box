package fiab.mes.mockactors.transport;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.handshake.actor.ClientHandshakeActor;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.iostation.MockIOStationFactory;
import fiab.mes.mockactors.transport.FUs.MockConveyorActor;
import fiab.mes.mockactors.transport.FUs.MockTurntableActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.transport.msg.InternalTransportModuleRequest;

public class TestTransportModuleCoordinatorActor { 

	protected static ActorSystem system;
	protected static ActorRef machine;
	public static String ROOT_SYSTEM = "routes";
	protected OrderProcess op;
	
	private static final Logger logger = LoggerFactory.getLogger(TestTransportModuleCoordinatorActor.class);
	
	private static boolean engageAutoReload = true;
	private static boolean disengageAutoReload = false;
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		
		system = ActorSystem.create(ROOT_SYSTEM);
		ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	}

	
	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test
	void testSetupMinimalShopfloor() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{
				
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				MockIOStationFactory partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload, 34);
				MockIOStationFactory partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload, 35);
				// now add to ttWrapper client Handshake actors
				ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
				ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
				ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				// setup turntable
				InterMachineEventBus intraEventBus = new InterMachineEventBus();	
				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				ActorRef turntableFU = system.actorOf(MockTurntableActor.props(intraEventBus, null), "TT1-TurntableFU");
				ActorRef conveyorFU = system.actorOf(MockConveyorActor.props(intraEventBus, null), "TT1-ConveyorFU");
				ActorRef ttWrapper = system.actorOf(TransportModuleCoordinatorActor.props(intraEventBus, turntableFU, conveyorFU), "TT1");
				ActorRef westClient = system.actorOf(ClientHandshakeActor.props(ttWrapper, inRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT); 
				ActorRef eastClient = system.actorOf(ClientHandshakeActor.props(ttWrapper, outRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT);
				
				boolean isProv = false;				
				ttWrapper.tell(new LocalEndpointStatus.LocalClientEndpointStatus(westClient, isProv, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT), getRef());
				ttWrapper.tell(new LocalEndpointStatus.LocalClientEndpointStatus(eastClient, isProv, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT), getRef());
				
				ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, getRef());				
				ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, getRef());
				
				boolean hasSentReq = false;
				boolean doRun = true;
				while (doRun) {
					MachineUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineUpdateEvent.class);
					logEvent(mue);
					if (mue instanceof MachineStatusUpdateEvent) {
						BasicMachineStates newState = BasicMachineStates.valueOf(((MachineStatusUpdateEvent) mue).getStatus().toString());
						if (newState.equals(BasicMachineStates.IDLE) && !hasSentReq) {
							ttWrapper.tell(new InternalTransportModuleRequest(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, "TestOrder1", "Req1"), getRef());
							hasSentReq = true;
						}
						if (newState.equals(BasicMachineStates.COMPLETE)) {
							doRun = false;
						}
					}
				}
			}
		};
	}
//	
//	@Test
//	void testUnknownCapSetupMinimalShopfloor() throws InterruptedException, ExecutionException {
//		new TestKit(system) { 
//			{
//				
//				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
//				MockIOStationFactory partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload, 34);
//				MockIOStationFactory partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload, 35);
//				// now add to ttWrapper client Handshake actors
//				ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
//				ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
//				ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
//				ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
//				// setup turntable
//				InterMachineEventBus intraEventBus = new InterMachineEventBus();	
//				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
//				ActorRef ttWrapper = system.actorOf(MockTransportModuleWrapper.props(intraEventBus), "TT1");
//				ActorRef westClient = system.actorOf(MockClientHandshakeActor.props(ttWrapper, inRef), "WestClient"); 
//				ActorRef eastClient = system.actorOf(MockClientHandshakeActor.props(ttWrapper, outRef), "EastClient");
//				
//				boolean isProv = false;
//				ttWrapper.tell(new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient, isProv, "WestClient"), getRef());
//				ttWrapper.tell(new MockTransportModuleWrapper.LocalClientEndpointStatus(eastClient, isProv, "EastClient"), getRef());
//				ttWrapper.tell(WellknownTransportModuleCapability.SimpleMessageTypes.SubscribeState, getRef());
//				ttWrapper.tell(WellknownTransportModuleCapability.SimpleMessageTypes.Reset, getRef());
//				
//				boolean doRun = true;
//				while (doRun) {
//					MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);
//					logEvent(mue);
//					MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
//					if (newState.equals(MachineStatus.IDLE)) {
//						ttWrapper.tell(new InternalTransportModuleRequest("WestClient", "BeastClient", "TestOrder1", "Req1"), getRef());
//					}
//					if (newState.equals(MachineStatus.STOPPED)) {
//						doRun = false;
//					}
//				}
//			}
//		};
//	}
//	
//	@Test
//	void testSetupTwoTurntablesMinimalShopfloor() throws InterruptedException, ExecutionException {
//		new TestKit(system) { 
//			{
//				
//				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
//				MockIOStationFactory partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload, 34);
//				MockIOStationFactory partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload, 35);
//				// now add to ttWrapper client Handshake actors
//				ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
//				ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
//				ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
//				ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
//				// setup turntable1
//				InterMachineEventBus intraEventBus1 = new InterMachineEventBus();	
//				intraEventBus1.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
//				ActorRef ttWrapper1 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus1), "TT1");
//				ActorRef westClient1 = system.actorOf(MockClientHandshakeActor.props(ttWrapper1, inRef), "WestClient1");
//				boolean autoComplete = true;
//				ActorRef eastServer1 = system.actorOf(MockServerHandshakeActor.props(ttWrapper1, autoComplete), "EastServer1");
//				boolean isReq = false;
//				boolean isProv = true;
//				ttWrapper1.tell( new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient1, isReq, "WestClient1"), getRef());
//				ttWrapper1.tell( new MockTransportModuleWrapper.LocalServerEndpointStatus(eastServer1, isProv, "EastServer1"), getRef());
//				ttWrapper1.tell(WellknownTransportModuleCapability.SimpleMessageTypes.SubscribeState, getRef());
//				ttWrapper1.tell(WellknownTransportModuleCapability.SimpleMessageTypes.Reset, getRef());
//				// setup turntable 2
//				InterMachineEventBus intraEventBus2 = new InterMachineEventBus();	
//				intraEventBus2.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
//				ActorRef ttWrapper2 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus2), "TT2");
//				ActorRef eastClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, outRef), "EastClient2");
//				ActorRef westClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, eastServer1), "WestClient2");
//				ttWrapper2.tell( new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient2, isReq, "WestClient2"), getRef());
//				ttWrapper2.tell( new MockTransportModuleWrapper.LocalClientEndpointStatus(eastClient2, isReq, "EastClient2"), getRef());
//				ttWrapper2.tell(WellknownTransportModuleCapability.SimpleMessageTypes.SubscribeState, getRef());
//				ttWrapper2.tell(WellknownTransportModuleCapability.SimpleMessageTypes.Reset, getRef());
//				
//
//				boolean tt1Done = false;
//				boolean tt2Done = false;
//				while (!(tt1Done && tt2Done)) {
//					MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);
//					logEvent(mue);
//					MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
//					if (newState.equals(MachineStatus.IDLE)) {
//						if (mue.getMachineId().equals("TT1") && !tt1Done)
//							ttWrapper1.tell(new InternalTransportModuleRequest("WestClient1", "EastServer1", "TestOrder1", "Req1"), getRef());
//						else if (!tt2Done)
//							ttWrapper2.tell(new InternalTransportModuleRequest("WestClient2", "EastClient2", "TestOrder1", "Req2"), getRef());
//					}
//					if (newState.equals(MachineStatus.COMPLETE)) {
//						if (mue.getMachineId().equals("TT1"))
//							tt1Done = true;
//						else 
//							tt2Done = true;
//					}
//				}
//			}
//		};
//	}
		
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}
	
	
	
}
