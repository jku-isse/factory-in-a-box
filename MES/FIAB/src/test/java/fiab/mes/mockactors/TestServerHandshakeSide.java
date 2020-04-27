package fiab.mes.mockactors;

import java.time.Duration;

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
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSide;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;

public class TestServerHandshakeSide { 

	protected static ActorSystem system;	
	public static String ROOT_SYSTEM = "routes";
	protected OrderProcess op;
	
	private static final Logger logger = LoggerFactory.getLogger(TestServerHandshakeSide.class);
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		
		system = ActorSystem.create(ROOT_SYSTEM);
		
	}

	
	
	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test
	void testServerHandshake() {		
		new TestKit(system) { 
			{
				boolean doAutoComplete = false;
				ActorRef serverSide = system.actorOf(MockServerHandshakeActor.props(getRef(), doAutoComplete), "ServerSide"); // we want to see events
				boolean done = false;
				serverSide.tell(IOStationCapability.ServerMessageTypes.Reset, getRef());
				while (!done) {
					ServerSide state = expectMsgClass(Duration.ofSeconds(5), ServerSide.class);
					logEvent(state);
					switch(state) {
					case IDLE_EMPTY:
						serverSide.tell(IOStationCapability.ServerMessageTypes.RequestInitiateHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.STARTING));
						logMsg(expectMsg(Duration.ofSeconds(5), IOStationCapability.ServerMessageTypes.OkResponseInitHandover));
						break;
					case READY_EMPTY:
						serverSide.tell(IOStationCapability.ServerMessageTypes.RequestStartHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.EXECUTE));
						logMsg(expectMsg(Duration.ofSeconds(5), IOStationCapability.ServerMessageTypes.OkResponseStartHandover));
						// here we play the FU signaling that handover is complete
						serverSide.tell(IOStationCapability.ServerMessageTypes.Complete, getRef());
						break;
					case COMPLETE:
						done = true; // end of the handshake cycle
						break;
					default:
						break;
					}
				}				
			}	
		};
	}
		
	@Test
	void testTwoSequentialServerHandshakes() {		
		new TestKit(system) { 
			{
				boolean doAutoComplete = false;
				ActorRef serverSide = system.actorOf(MockServerHandshakeActor.props(getRef(), doAutoComplete), "ServerSide"); // we want to see events
				boolean done = false;
				serverSide.tell(IOStationCapability.ServerMessageTypes.Reset, getRef());
				while (!done) {
					ServerSide state = expectMsgClass(Duration.ofSeconds(5), ServerSide.class);
					logEvent(state);
					switch(state) {
					case IDLE_EMPTY:
						serverSide.tell(IOStationCapability.ServerMessageTypes.RequestInitiateHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.STARTING));
						logMsg(expectMsg(Duration.ofSeconds(5), IOStationCapability.ServerMessageTypes.OkResponseInitHandover));
						break;
					case READY_EMPTY:
						serverSide.tell(IOStationCapability.ServerMessageTypes.RequestStartHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.EXECUTE));
						logMsg(expectMsg(Duration.ofSeconds(5), IOStationCapability.ServerMessageTypes.OkResponseStartHandover));
						// here we play the FU signaling that handover is complete
						serverSide.tell(IOStationCapability.ServerMessageTypes.Complete, getRef());
						break;
					case COMPLETE:
						done = true; // end of the handshake cycle
						break;
					default:
						break;
					}
				}	
				done = false;
				serverSide.tell(IOStationCapability.ServerMessageTypes.Reset, getRef());
				while (!done) {
					ServerSide state = expectMsgClass(Duration.ofSeconds(5), ServerSide.class);
					logEvent(state);
					switch(state) {
					case IDLE_LOADED:
						serverSide.tell(IOStationCapability.ServerMessageTypes.RequestInitiateHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.STARTING));
						logMsg(expectMsg(Duration.ofSeconds(5), IOStationCapability.ServerMessageTypes.OkResponseInitHandover));
						break;
					case READY_LOADED:
						serverSide.tell(IOStationCapability.ServerMessageTypes.RequestStartHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.EXECUTE));
						logMsg(expectMsg(Duration.ofSeconds(5), IOStationCapability.ServerMessageTypes.OkResponseStartHandover));
						// here we play the FU signaling that handover is complete
						serverSide.tell(IOStationCapability.ServerMessageTypes.Complete, getRef());
						break;
					case COMPLETE:
						done = true; // end of the handshake cycle
						break;
					default:
						break;
					}
				}
			}	
		};
	}
	
	private void logEvent(ServerSide event) {
		logger.info(event.toString());
	}
	
	private void logMsg(IOStationCapability.ServerMessageTypes event) {
		logger.info(event.toString());
	}
	
}
