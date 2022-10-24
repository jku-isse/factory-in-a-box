package fiab.mes.mockactors.iostation;

import java.time.Duration;

import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.shopfloor.layout.ShopfloorLayout;
import fiab.mes.shopfloor.layout.SingleTurntableLayout;
import org.junit.jupiter.api.*;
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

import static fiab.mes.shopfloor.utils.ShopfloorUtils.INPUT_STATION;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.OUTPUT_STATION;

@Tag("IntegrationTest")
public class TestBasicIOStationActorWithTransport { 

	protected ActorSystem system;
	protected ActorRef machine;
	public String ROOT_SYSTEM = "routes";
	private ActorRef interMachineEventBus;
	
	private static final Logger logger = LoggerFactory.getLogger(TestBasicIOStationActorWithTransport.class);
	
	@BeforeEach
	public void setUpBeforeClass() throws Exception {
		system = ActorSystem.create(ROOT_SYSTEM);
		interMachineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	}

	@AfterEach
	public void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test
	void testBasicInputStationToIdleEmpty() {
		new TestKit(system) { 
			{
				ShopfloorLayout layout = new SingleTurntableLayout(system, interMachineEventBus);
				layout.subscribeToInterMachineEventBus(getRef(), getRef().path().name());
				layout.initializeAndDiscoverParticipantsForId(getRef(), INPUT_STATION);

				MachineConnectedEvent connectedEvent = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class);
				logEvent(connectedEvent);
				AkkaActorBackedCoreModelAbstractActor inputStation = connectedEvent.getMachine();

				boolean doRun = true;
				while (doRun) {
					IOStationStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(30), IOStationStatusUpdateEvent.class);
					logEvent(mue);
					if (mue.getStatus().equals(ServerSideStates.RESETTING)) {
						inputStation.getAkkaActor().tell(HandshakeCapability.StateOverrideRequests.SetLoaded, getRef());
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
				ShopfloorLayout layout = new SingleTurntableLayout(system, interMachineEventBus);
				layout.subscribeToInterMachineEventBus(getRef(), getRef().path().name());
				layout.initializeAndDiscoverParticipantsForId(getRef(), OUTPUT_STATION);

				MachineConnectedEvent connectedEvent = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class);
				logEvent(connectedEvent);
				AkkaActorBackedCoreModelAbstractActor outputStation = connectedEvent.getMachine();
				boolean doRun = true;
				while (doRun) {
					IOStationStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(30), IOStationStatusUpdateEvent.class);
					logEvent(mue);
					if (mue.getStatus().equals(ServerSideStates.RESETTING)) {
						outputStation.getAkkaActor().tell(HandshakeCapability.StateOverrideRequests.SetEmpty, getRef());
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
