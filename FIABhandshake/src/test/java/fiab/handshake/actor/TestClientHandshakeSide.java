package fiab.handshake.actor;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.handshake.actor.ClientHandshakeActor;

public class TestClientHandshakeSide { 

	protected static ActorSystem system;	
	public static String ROOT_SYSTEM = "routes";
	
	private static final Logger logger = LoggerFactory.getLogger(TestClientHandshakeSide.class);
	
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
	void testClientHandshake() {		
		new TestKit(system) { 
			{
				ActorRef clientSide = system.actorOf(ClientHandshakeActor.props(getRef(), getRef()), "ClientSide"); // we want to see events
				boolean done = false;
				clientSide.tell(HandshakeCapability.ClientMessageTypes.Reset, getRef());
				while (!done) {
					ClientSideStates state = expectMsgClass(Duration.ofSeconds(5), ClientSideStates.class);
					logEvent(state);
					switch(state) {
					case IDLE:
						clientSide.tell(HandshakeCapability.ClientMessageTypes.Start, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ClientSideStates.STARTING));
						logMsg(expectMsg(Duration.ofSeconds(5), HandshakeCapability.ServerMessageTypes.SubscribeToStateUpdates));
						logEvent(expectMsg(Duration.ofSeconds(5), ClientSideStates.INITIATING));
						// now we send our update, lets assume first we are resetting, then idle
						clientSide.tell(ServerSideStates.RESETTING, getRef());
						clientSide.tell(ServerSideStates.IDLE_EMPTY, getRef());
						logMsg(expectMsg(Duration.ofSeconds(5), HandshakeCapability.ServerMessageTypes.RequestInitiateHandover));
						clientSide.tell(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover, getRef());
						break;
					case READY:
						clientSide.tell(ServerSideStates.READY_EMPTY, getRef());
						logMsg(expectMsg(Duration.ofSeconds(5), HandshakeCapability.ServerMessageTypes.RequestStartHandover));
						clientSide.tell(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover, getRef());
						break;
					case EXECUTE: // now we tell to complete playing the local FU
						clientSide.tell(HandshakeCapability.ClientMessageTypes.Complete, getRef());
					case COMPLETED:
						done = true; // end of the handshake cycle
						break;
					default:
						break;
					}
				}				
			}	
		};
	}
		
	
	
	private void logEvent(ClientSideStates event) {
		logger.info(event.toString());
	}
	
	private void logMsg(HandshakeCapability.ServerMessageTypes event) {
		logger.info(event.toString());
	}
	
}
