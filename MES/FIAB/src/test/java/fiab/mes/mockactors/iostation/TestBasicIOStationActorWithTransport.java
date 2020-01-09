package fiab.mes.mockactors.iostation;

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
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
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
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;

public class TestBasicIOStationActorWithTransport { 

	protected static ActorSystem system;
	protected static ActorRef machine;
	public static String ROOT_SYSTEM = "routes";
	
	private static final Logger logger = LoggerFactory.getLogger(TestBasicIOStationActorWithTransport.class);
	
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
	void testBasicInputStationToIdleEmpty() {
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				MockIOStationFactory parts = MockIOStationFactory.getMockedInputStation(system, eventBusByRef,false, 34);
				// we subscribe to the intereventbus to observe basic io station behavior
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				//eventBusByRef.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class));
				boolean doRun = true;
				while (doRun) {
					IOStationStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), IOStationStatusUpdateEvent.class);
					logEvent(mue);
					if (mue.getStatus().equals(ServerSide.IdleLoaded)) {
						doRun = false;
					}
				}
			}	
		};
	}
		
	@Test
	void testBasicOutputStationToIdleEmpty() {
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				MockIOStationFactory parts = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, false, 35);
				// we subscribe to the intereventbus to observe basic io station behavior
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class));
				//eventBusByRef.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				boolean doRun = true;
				while (doRun) {
					IOStationStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), IOStationStatusUpdateEvent.class);
					logEvent(mue);
					if (mue.getStatus().equals(ServerSide.IdleEmpty)) {
						doRun = false;
					}
				}
			}	
		};
	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}
	

	
}
