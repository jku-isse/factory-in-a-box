package fiab.handshake.actor;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.handshake.actor.ServerSideHandshakeActor;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;

public class TestServerHandshakeSide { 

	protected static ActorSystem system;	
	public static String ROOT_SYSTEM = "routes";
	
	private static final Logger logger = LoggerFactory.getLogger(TestServerHandshakeSide.class);
	
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
	void testServerHandshake() {		
		new TestKit(system) { 
			{
				boolean doAutoComplete = false;
				ActorRef serverSide = system.actorOf(ServerSideHandshakeActor.props(getRef(), doAutoComplete), "ServerSide"); // we want to see events
				boolean done = false;
				serverSide.tell(HandshakeCapability.ServerMessageTypes.Reset, getRef());
				while (!done) {
					ServerSideStates state = expectMsgClass(Duration.ofSeconds(5), ServerSideStates.class);
					logEvent(state);
					switch(state) {
					case IDLE_EMPTY:
						serverSide.tell(HandshakeCapability.ServerMessageTypes.RequestInitiateHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSideStates.STARTING));
						logMsg(expectMsg(Duration.ofSeconds(5), HandshakeCapability.ServerMessageTypes.OkResponseInitHandover));
						break;
					case READY_EMPTY:
						serverSide.tell(HandshakeCapability.ServerMessageTypes.RequestStartHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSideStates.EXECUTE));
						logMsg(expectMsg(Duration.ofSeconds(5), HandshakeCapability.ServerMessageTypes.OkResponseStartHandover));
						// here we play the FU signaling that handover is complete
						serverSide.tell(HandshakeCapability.ServerMessageTypes.Complete, getRef());
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
				ActorRef serverSide = system.actorOf(ServerSideHandshakeActor.props(getRef(), doAutoComplete), "ServerSide"); // we want to see events
				boolean done = false;
				serverSide.tell(HandshakeCapability.ServerMessageTypes.Reset, getRef());
				while (!done) {
					ServerSideStates state = expectMsgClass(Duration.ofSeconds(5), ServerSideStates.class);
					logEvent(state);
					switch(state) {
					case IDLE_EMPTY:
						serverSide.tell(HandshakeCapability.ServerMessageTypes.RequestInitiateHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSideStates.STARTING));
						logMsg(expectMsg(Duration.ofSeconds(5), HandshakeCapability.ServerMessageTypes.OkResponseInitHandover));
						break;
					case READY_EMPTY:
						serverSide.tell(HandshakeCapability.ServerMessageTypes.RequestStartHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSideStates.EXECUTE));
						logMsg(expectMsg(Duration.ofSeconds(5), HandshakeCapability.ServerMessageTypes.OkResponseStartHandover));
						// here we play the FU signaling that handover is complete
						serverSide.tell(HandshakeCapability.ServerMessageTypes.Complete, getRef());
						break;
					case COMPLETE:
						done = true; // end of the handshake cycle
						break;
					default:
						break;
					}
				}	
				done = false;
				serverSide.tell(HandshakeCapability.ServerMessageTypes.Reset, getRef());
				while (!done) {
					ServerSideStates state = expectMsgClass(Duration.ofSeconds(5), ServerSideStates.class);
					logEvent(state);
					switch(state) {
					case IDLE_LOADED:
						serverSide.tell(HandshakeCapability.ServerMessageTypes.RequestInitiateHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSideStates.STARTING));
						logMsg(expectMsg(Duration.ofSeconds(5), HandshakeCapability.ServerMessageTypes.OkResponseInitHandover));
						break;
					case READY_LOADED:
						serverSide.tell(HandshakeCapability.ServerMessageTypes.RequestStartHandover, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ServerSideStates.EXECUTE));
						logMsg(expectMsg(Duration.ofSeconds(5), HandshakeCapability.ServerMessageTypes.OkResponseStartHandover));
						// here we play the FU signaling that handover is complete
						serverSide.tell(HandshakeCapability.ServerMessageTypes.Complete, getRef());
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
	
	private void logEvent(ServerSideStates event) {
		logger.info(event.toString());
	}
	
	private void logMsg(HandshakeCapability.ServerMessageTypes event) {
		logger.info(event.toString());
	}
	
}
