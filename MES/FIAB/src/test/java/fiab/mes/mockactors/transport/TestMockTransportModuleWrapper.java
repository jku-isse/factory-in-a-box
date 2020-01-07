package fiab.mes.mockactors.transport;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.ProcessStep;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.ComparableCapability;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockClientHandshakeActor;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.mockactors.MockServerHandshakeActor.MessageTypes;
import fiab.mes.mockactors.iostation.MockIOStationFactory;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper.LocalEndpointStatus;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;

public class TestMockTransportModuleWrapper { 

	protected static ActorSystem system;
	protected static ActorRef machine;
	public static String ROOT_SYSTEM = "routes";
	protected OrderProcess op;
	
	private static final Logger logger = LoggerFactory.getLogger(TestMockTransportModuleWrapper.class);
	
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
				MockIOStationFactory partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload);
				MockIOStationFactory partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload);
				// now add to ttWrapper client Handshake actors
				ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
				ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
				ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				// setup turntable
				InterMachineEventBus intraEventBus = new InterMachineEventBus();	
				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				ActorRef ttWrapper = system.actorOf(MockTransportModuleWrapper.props(intraEventBus), "TT1");
				ActorRef westClient = system.actorOf(MockClientHandshakeActor.props(ttWrapper, inRef), "WestClient"); 
				ActorRef eastClient = system.actorOf(MockClientHandshakeActor.props(ttWrapper, outRef), "EastClient");
				Map<String, LocalEndpointStatus> eps = new HashMap<>();
				boolean isProv = false;
				eps.put("WestClient", new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient, isProv, "WestClient"));
				eps.put("EastClient", new MockTransportModuleWrapper.LocalClientEndpointStatus(eastClient, isProv, "EastClient"));
				ttWrapper.tell(new MockTransportModuleWrapper.HandshakeEndpointInfo(eps), getRef());
				ttWrapper.tell(MockTransportModuleWrapper.SimpleMessageTypes.SubscribeState, getRef());
				ttWrapper.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, getRef());
				
				boolean doRun = true;
				while (doRun) {
					MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);
					logEvent(mue);
					MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
					if (newState.equals(MachineStatus.IDLE)) {
						ttWrapper.tell(new TransportModuleRequest("WestClient", "EastClient", "TestOrder1"), getRef());
					}
					if (newState.equals(MachineStatus.COMPLETE)) {
						doRun = false;
					}
				}
			}
		};
	}
	
	@Test
	void testUnknownCapSetupMinimalShopfloor() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{
				
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				MockIOStationFactory partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload);
				MockIOStationFactory partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload);
				// now add to ttWrapper client Handshake actors
				ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
				ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
				ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				// setup turntable
				InterMachineEventBus intraEventBus = new InterMachineEventBus();	
				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				ActorRef ttWrapper = system.actorOf(MockTransportModuleWrapper.props(intraEventBus), "TT1");
				ActorRef westClient = system.actorOf(MockClientHandshakeActor.props(ttWrapper, inRef), "WestClient"); 
				ActorRef eastClient = system.actorOf(MockClientHandshakeActor.props(ttWrapper, outRef), "EastClient");
				Map<String, LocalEndpointStatus> eps = new HashMap<>();
				boolean isProv = false;
				eps.put("WestClient", new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient, isProv, "WestClient"));
				eps.put("EastClient", new MockTransportModuleWrapper.LocalClientEndpointStatus(eastClient, isProv, "EastClient"));
				ttWrapper.tell(new MockTransportModuleWrapper.HandshakeEndpointInfo(eps), getRef());
				ttWrapper.tell(MockTransportModuleWrapper.SimpleMessageTypes.SubscribeState, getRef());
				ttWrapper.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, getRef());
				
				boolean doRun = true;
				while (doRun) {
					MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);
					logEvent(mue);
					MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
					if (newState.equals(MachineStatus.IDLE)) {
						ttWrapper.tell(new TransportModuleRequest("WestClient", "BeastClient", "TestOrder1"), getRef());
					}
					if (newState.equals(MachineStatus.STOPPED)) {
						doRun = false;
					}
				}
			}
		};
	}
	
	@Test
	void testSetupTwoTurntablesMinimalShopfloor() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{
				
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				MockIOStationFactory partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload);
				MockIOStationFactory partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload);
				// now add to ttWrapper client Handshake actors
				ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
				ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
				ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				// setup turntable1
				InterMachineEventBus intraEventBus1 = new InterMachineEventBus();	
				intraEventBus1.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				ActorRef ttWrapper1 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus1), "TT1");
				ActorRef westClient1 = system.actorOf(MockClientHandshakeActor.props(ttWrapper1, inRef), "WestClient1");
				boolean autoComplete = true;
				ActorRef eastServer1 = system.actorOf(MockServerHandshakeActor.props(ttWrapper1, autoComplete), "EastServer1");
				Map<String, LocalEndpointStatus> eps1 = new HashMap<>();
				boolean isReq = false;
				boolean isProv = true;
				eps1.put("WestClient1", new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient1, isReq, "WestClient1"));
				eps1.put("EastServer1", new MockTransportModuleWrapper.LocalServerEndpointStatus(eastServer1, isProv, "EastServer1"));
				ttWrapper1.tell(new MockTransportModuleWrapper.HandshakeEndpointInfo(eps1), getRef());
				ttWrapper1.tell(MockTransportModuleWrapper.SimpleMessageTypes.SubscribeState, getRef());
				ttWrapper1.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, getRef());
				// setup turntable 2
				InterMachineEventBus intraEventBus2 = new InterMachineEventBus();	
				intraEventBus2.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				ActorRef ttWrapper2 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus2), "TT2");
				ActorRef eastClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, outRef), "EastClient2");
				ActorRef westClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, eastServer1), "WestClient2");
				Map<String, LocalEndpointStatus> eps2 = new HashMap<>();
				eps2.put("WestClient2", new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient2, isReq, "WestClient2"));
				eps2.put("EastClient2", new MockTransportModuleWrapper.LocalClientEndpointStatus(eastClient2, isReq, "EastClient2"));
				ttWrapper2.tell(new MockTransportModuleWrapper.HandshakeEndpointInfo(eps2), getRef());
				ttWrapper2.tell(MockTransportModuleWrapper.SimpleMessageTypes.SubscribeState, getRef());
				ttWrapper2.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, getRef());
				

				boolean tt1Done = false;
				boolean tt2Done = false;
				while (!(tt1Done && tt2Done)) {
					MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);
					logEvent(mue);
					MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
					if (newState.equals(MachineStatus.IDLE)) {
						if (mue.getMachineId().equals("TT1"))
							ttWrapper1.tell(new TransportModuleRequest("WestClient1", "EastServer1", "TestOrder1"), getRef());
						else
							ttWrapper2.tell(new TransportModuleRequest("WestClient2", "EastClient2", "TestOrder1"), getRef());
					}
					if (newState.equals(MachineStatus.COMPLETE)) {
						if (mue.getMachineId().equals("TT1"))
							tt1Done = true;
						else 
							tt2Done = true;
					}
				}
			}
		};
	}
	
//	@Test
//	void testPlotAtMachineWithServerSideAutoComplete() {
//		new TestKit(system) { 
//			{
//				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
//				//eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
//				
//				InterMachineEventBus intraEventBus = new InterMachineEventBus();
//				ActorRef machineWrapper = system.actorOf(MockTransportAwareMachineWrapper.props(intraEventBus), "MachineWrapper1");
//				ActorSelection serverSide = system.actorSelection("/user/MachineWrapper1/ServerSideHandshakeMock");
//				PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
//				machine = system.actorOf(BasicMachineActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
//				// we subscribe to the intraeventbus to observe wrapper behavior
//				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
//				
//				boolean doRun = true;
//				while (doRun) {
//					MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);
//					logEvent(mue);
//					MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
//					if (newState.equals(MachineStatus.IDLE)) {
//						machine.tell(new LockForOrder("TestStep1","TestRootOrder1"), getRef()); // here we dont register and wait for readyness, wont work later
//					}
//					if (newState.equals(MachineStatus.STARTING)) {
//						boolean handshakeDone = false;
//						serverSide.tell(MockServerHandshakeActor.MessageTypes.SubscribeToStateUpdates, getRef());
//						while (!handshakeDone) {
//							ServerSide state = expectMsgClass(Duration.ofSeconds(5), ServerSide.class);
//							switch(state) {
//							case IdleEmpty:
//								serverSide.tell(MockServerHandshakeActor.MessageTypes.RequestInitiateHandover, getRef());
//								expectMsg(Duration.ofSeconds(5), ServerSide.Starting);
//								expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseInitHandover);
//								break;
//							case ReadyEmpty:
//								serverSide.tell(MessageTypes.RequestStartHandover, getRef());
//								expectMsg(Duration.ofSeconds(5), ServerSide.Execute);
//								expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseStartHandover);
//								serverSide.tell(MockServerHandshakeActor.MessageTypes.UnsubscribeToStateUpdates, getRef()); //otherwise the handshake events interfere with other expected events
//								handshakeDone = true; // part until where we need to be involved, thanks to autocomplete
//								break;
//							default:
//								break;
//							}
//						}
//					}
//					if (newState.equals(MachineStatus.COMPLETING)) {
//						doRun = false;
//					}
//				}
//				//expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
//			}	
//		};
//	}
		
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}
	
	
	
}
