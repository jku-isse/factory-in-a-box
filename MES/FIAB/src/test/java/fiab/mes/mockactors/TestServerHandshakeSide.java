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
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.ComparableCapability;
import fiab.mes.general.TimedEvent;
import fiab.mes.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockServerHandshakeActor.MessageTypes;
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
				ActorRef serverSide = system.actorOf(MockServerHandshakeActor.props(getRef()), "ServerSide"); // we want to see events
				boolean done = false;
				serverSide.tell(MessageTypes.Reset, getRef());
				while (!done) {
					ServerSide state = expectMsgClass(Duration.ofSeconds(5), ServerSide.class);
					logEvent(state);
					switch(state) {
					case IdleEmpty:
						serverSide.tell(MessageTypes.RequestInitiateHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.Starting));
						logMsg(expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseInitHandover));
						break;
					case ReadyEmpty:
						serverSide.tell(MessageTypes.RequestStartHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.Execute));
						logMsg(expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseStartHandover));
						// here we play the FU signaling that handover is complete
						serverSide.tell(MessageTypes.Complete, getRef());
						break;
					case Stopping:
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
				ActorRef serverSide = system.actorOf(MockServerHandshakeActor.props(getRef()), "ServerSide"); // we want to see events
				boolean done = false;
				serverSide.tell(MessageTypes.Reset, getRef());
				while (!done) {
					ServerSide state = expectMsgClass(Duration.ofSeconds(5), ServerSide.class);
					logEvent(state);
					switch(state) {
					case IdleEmpty:
						serverSide.tell(MessageTypes.RequestInitiateHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.Starting));
						logMsg(expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseInitHandover));
						break;
					case ReadyEmpty:
						serverSide.tell(MessageTypes.RequestStartHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.Execute));
						logMsg(expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseStartHandover));
						// here we play the FU signaling that handover is complete
						serverSide.tell(MessageTypes.Complete, getRef());
						break;
					case Stopping:
						done = true; // end of the handshake cycle
						break;
					default:
						break;
					}
				}	
				done = false;
				serverSide.tell(MessageTypes.Reset, getRef());
				while (!done) {
					ServerSide state = expectMsgClass(Duration.ofSeconds(5), ServerSide.class);
					logEvent(state);
					switch(state) {
					case IdleLoaded:
						serverSide.tell(MessageTypes.RequestInitiateHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.Starting));
						logMsg(expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseInitHandover));
						break;
					case ReadyLoaded:
						serverSide.tell(MessageTypes.RequestStartHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSide.Execute));
						logMsg(expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseStartHandover));
						// here we play the FU signaling that handover is complete
						serverSide.tell(MessageTypes.Complete, getRef());
						break;
					case Stopping:
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
	
	private void logMsg(MessageTypes event) {
		logger.info(event.toString());
	}
	
}
