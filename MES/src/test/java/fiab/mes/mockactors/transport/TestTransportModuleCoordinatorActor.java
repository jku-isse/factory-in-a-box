package fiab.mes.mockactors.transport;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.handshake.actor.ClientHandshakeActor;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.mockactors.iostation.VirtualIOStationActorFactory;
import fiab.mes.order.OrderProcess;
import fiab.mes.shopfloor.DefaultLayout;
import fiab.turntable.actor.InternalTransportModuleRequest;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.actor.SubscriptionClassifier;
import fiab.turntable.actor.TransportModuleCoordinatorActor;
import fiab.turntable.conveying.BaseBehaviorConveyorActor;
import fiab.turntable.conveying.ConveyorActor;
import fiab.turntable.turning.BaseBehaviorTurntableActor;
import fiab.turntable.turning.TurntableActor;

public class TestTransportModuleCoordinatorActor { 

	protected static ActorSystem system;
	protected static ActorRef machine;
	public static String ROOT_SYSTEM = "TEST_TTINTEGRATION";
	protected OrderProcess op;
	
	private static final Logger logger = LoggerFactory.getLogger(TestTransportModuleCoordinatorActor.class);
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {		
		system = ActorSystem.create(ROOT_SYSTEM);
	}

	
	@AfterAll
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	

	@Test
	void testSetupMinimalShopfloor() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{
				DefaultLayout layout = new DefaultLayout(system);
				layout.setupIOStations(34, 35);
				ActorSelection inServer = system.actorSelection("/user/"+layout.partsIn.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
				ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				ActorSelection outServer = system.actorSelection("/user/"+layout.partsOut.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
				ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				// setup turntable
				IntraMachineEventBus intraEventBus = new IntraMachineEventBus();	
				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				Map<String,ActorRef> ioRefs = new HashMap<String,ActorRef>();
				ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, inRef);
				ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, outRef);
				ActorRef ttWrapper = layout.setupSingleTurntable(1, intraEventBus, ioRefs, new HashSet<String>());
				ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, getRef());				
				ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, getRef());
				
				boolean hasSentReq = false;
				boolean doRun = true;
				while (doRun) {
					MachineUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineUpdateEvent.class);
					logEvent(mue);
					if (mue instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) mue).getStatus().equals(BasicMachineStates.IDLE) && !hasSentReq) {
							ttWrapper.tell(new InternalTransportModuleRequest(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, "TestOrder1", "Req1"), getRef());
							hasSentReq = true;
						}
						if (((MachineStatusUpdateEvent) mue).getStatus().equals(BasicMachineStates.COMPLETE)) {
							doRun = false;
						}
					}
				}
			}
		};
	}
	
	
	@Test
	void testSetup2TTplusIO() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{
				DefaultLayout layout = new DefaultLayout(system);
				layout.setupIOStations(34, 35);
				ActorSelection inServer = system.actorSelection("/user/"+layout.partsIn.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
				ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				ActorSelection outServer = system.actorSelection("/user/"+layout.partsOut.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
				ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				// setup turntable
				IntraMachineEventBus intraEventBus = new IntraMachineEventBus();	
				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				Map<String,ActorRef> iRefs = new HashMap<String,ActorRef>();
				iRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, inRef);
				Set<String> serverRefs = new HashSet<String>();
				serverRefs.add(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER);
				ActorRef ttWrapper = layout.setupSingleTurntable(1, intraEventBus, iRefs, serverRefs);
				ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, getRef());				
				ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, getRef());
				
				IntraMachineEventBus intraEventBus2 = new IntraMachineEventBus();	
				intraEventBus2.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				Map<String,ActorRef> ioRefs = new HashMap<String,ActorRef>();
				ActorSelection eastServerSel = system.actorSelection("/user/"+TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER+"~"+1);
				ActorRef eastServer = eastServerSel.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, outRef);
				ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, eastServer);
				ActorRef ttWrapper2 = layout.setupSingleTurntable(2, intraEventBus2, ioRefs, new HashSet<String>());
				ttWrapper2.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, getRef());				
				ttWrapper2.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, getRef());
				
				boolean hasSentReqTT1 = false;
				boolean hasSentReqTT2 = false;
				boolean doRun = true;
				while (doRun) {
					MachineUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineUpdateEvent.class);
					logEvent(mue);
					if (mue instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent)mue;
						if (msue.getMachineId().equals("TT1") && msue.getStatus().equals(BasicMachineStates.IDLE) && !hasSentReqTT1) {
							ttWrapper.tell(new InternalTransportModuleRequest(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER, "TestOrder1", "Req1"), getRef());
							hasSentReqTT1 = true;
						}
						if (msue.getMachineId().equals("TT2") && msue.getStatus().equals(BasicMachineStates.IDLE) && !hasSentReqTT2) {
							ttWrapper2.tell(new InternalTransportModuleRequest(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, "TestOrder2", "Req2"), getRef());
							hasSentReqTT2 = true;
						}
						if (msue.getMachineId().equals("TT2") && msue.getStatus().equals(BasicMachineStates.COMPLETE)) {
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
