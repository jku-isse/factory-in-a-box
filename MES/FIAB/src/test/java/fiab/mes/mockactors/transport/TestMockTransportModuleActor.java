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
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.handshake.actor.ClientHandshakeActor;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.ServerSideHandshakeActor;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.iostation.MockIOStationFactory;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.msg.InternalTransportModuleRequest;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;

public class TestMockTransportModuleActor { 

	protected static ActorSystem system;
	protected static ActorRef machine;
	public static String ROOT_SYSTEM = "routes";
	protected OrderProcess op;
	
	private static final Logger logger = LoggerFactory.getLogger(TestMockTransportModuleActor.class);
	
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
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				
				MockIOStationFactory partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, disengageAutoReload, 20);
				MockIOStationFactory partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, disengageAutoReload, 35);
				// now add to ttWrapper client Handshake actors
				ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
				ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
				ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
				
				// setup turntable
				InterMachineEventBus intraEventBus = new InterMachineEventBus();	
				//intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*")); --> we listen to external machine event bus
				ActorRef ttWrapper = system.actorOf(MockTransportModuleWrapper.props(intraEventBus), "TT1HWMockActor");
				ActorRef westClient = system.actorOf(ClientHandshakeActor.props(ttWrapper, inRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT); 
				ActorRef eastClient = system.actorOf(ClientHandshakeActor.props(ttWrapper, outRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT);
				
				boolean isProv = false;
				ttWrapper.tell( new LocalEndpointStatus.LocalClientEndpointStatus(westClient, isProv, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT), getRef());
				ttWrapper.tell( new LocalEndpointStatus.LocalClientEndpointStatus(eastClient, isProv, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT), getRef());
				ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, getRef());
				ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, getRef());
				// setup actual turntable actor:
				
				int ipid = 21;
				AbstractCapability cap = TransportModuleCapability.getTransportCapability();
				Actor modelActor = getDefaultTransportModuleModelActor(ipid);
				MockTransportModuleWrapperDelegate hal = new MockTransportModuleWrapperDelegate(ttWrapper);
				Position selfPos = new Position(ipid+"");
				HardcodedDefaultTransportRoutingAndMapping env = new HardcodedDefaultTransportRoutingAndMapping();								
				ActorRef ttActor = system.actorOf(BasicTransportModuleActor.props(eventBusByRef, cap, modelActor, hal, selfPos, intraEventBus, new TransportPositionLookup(), env), "TT1Actor");
				
				// connect and status events of io stations and turntable
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class));
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class));
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class));
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class));
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class));
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class));
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class));
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class));
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class));
				boolean doRun = true;
				int rounds = 0;
				while (doRun && rounds < 1) {					
					Object msg = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class,  IOStationStatusUpdateEvent.class);
					//MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);					
					logEvent((TimedEvent) msg);
					if (msg instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent mue = (MachineStatusUpdateEvent) msg;
						BasicMachineStates newState = BasicMachineStates.valueOf(mue.getStatus().toString());
						if (newState.equals(BasicMachineStates.IDLE)) {
							ttActor.tell(new TransportModuleRequest(null, new Position("20"), new Position("35"), "TestOrder1", "TestReq1"), getRef()); // this only work because TT2 is configured to be a server
							//ttWrapper.tell(new InternalTransportModuleRequest("WestClient", "EastClient", "TestOrder1"), getRef());
						}
						if (newState.equals(BasicMachineStates.COMPLETE)) {
							doRun = false;
							rounds++;
						}
					}
				}
			}
		};
	}

	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}
	

	
	public static Actor getDefaultTransportModuleModelActor(int lastIPAddrPos) {
		Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
		actor.setID("MockTurntableActor"+lastIPAddrPos);
		actor.setActorName("MockTurntableActor"+lastIPAddrPos);
		actor.setDisplayName("MockTurntableActor"+lastIPAddrPos);
		actor.setUri("http://192.168.0."+lastIPAddrPos+":4840/MockTurntableActor"+lastIPAddrPos);
		return actor;
	}
	
}
