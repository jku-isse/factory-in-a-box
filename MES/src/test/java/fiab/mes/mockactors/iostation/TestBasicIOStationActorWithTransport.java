package fiab.mes.mockactors.iostation;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;

@Tag("IntegrationTest")
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

	@AfterAll
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test
	void testBasicInputStationToIdleEmpty() {
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );

				VirtualIOStationActorFactory parts = VirtualIOStationActorFactory.getInputStationForPosition(system, eventBusByRef,true, 34);
				// we subscribe to the intereventbus to observe basic io station behavior
				//parts.machine.tell(new GenericMachineRequests.Reset(""), getRef()); //RESET
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class));
				boolean doRun = true;
				while (doRun) {
					IOStationStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(30), IOStationStatusUpdateEvent.class);
					logEvent(mue);
					if (mue.getStatus().equals(ServerSideStates.RESETTING)) {
						parts.proxy.tell(HandshakeCapability.StateOverrideRequests.SetLoaded, getRef());
					}
					if (mue.getStatus().equals(ServerSideStates.IDLE_LOADED)) {
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
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );

				VirtualIOStationActorFactory parts = VirtualIOStationActorFactory.getOutputStationForPosition(system, eventBusByRef, false, 35);
				// we subscribe to the intereventbus to observe basic io station behavior
				parts.machine.tell(new GenericMachineRequests.Reset(""), getRef()); //RESET
				logEvent(expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class));
				boolean doRun = true;
				while (doRun) {
					IOStationStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(30), IOStationStatusUpdateEvent.class);
					logEvent(mue);
					if (mue.getStatus().equals(ServerSideStates.RESETTING)) {
						parts.proxy.tell(HandshakeCapability.StateOverrideRequests.SetEmpty, getRef());
					}
					if (mue.getStatus().equals(ServerSideStates.IDLE_EMPTY)) {
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
