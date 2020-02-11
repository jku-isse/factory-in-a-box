package fiab.mes.mockactors;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.mockactors.MockClientHandshakeActor.MessageTypes;
import fiab.mes.order.OrderProcess;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientSide;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class TestClientHandshakeSide { 

	protected static ActorSystem system;	
	public static String ROOT_SYSTEM = "routes";
	protected OrderProcess op;
	
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
				ActorRef clientSide = system.actorOf(MockClientHandshakeActor.props(getRef(), getRef()), "ClientSide"); // we want to see events
				boolean done = false;
				clientSide.tell(MessageTypes.Reset, getRef());
				while (!done) {
					ClientSide state = expectMsgClass(Duration.ofSeconds(5), ClientSide.class);
					logEvent(state);
					switch(state) {
					case IDLE:
						clientSide.tell(MessageTypes.Start, getRef());
						logEvent(expectMsg(Duration.ofSeconds(5), ClientSide.STARTING));
						logMsg(expectMsg(Duration.ofSeconds(5), MockServerHandshakeActor.MessageTypes.SubscribeToStateUpdates));
						logEvent(expectMsg(Duration.ofSeconds(5), ClientSide.INITIATING));
						// now we send our update, lets assume first we are resetting, then idle
						clientSide.tell(ServerSide.RESETTING, getRef());
						clientSide.tell(ServerSide.IDLE_EMPTY, getRef());
						logMsg(expectMsg(Duration.ofSeconds(5), MockServerHandshakeActor.MessageTypes.RequestInitiateHandover));
						clientSide.tell(MockServerHandshakeActor.MessageTypes.OkResponseInitHandover, getRef());
						break;
					case READY:
						clientSide.tell(ServerSide.READY_EMPTY, getRef());
						logMsg(expectMsg(Duration.ofSeconds(5), MockServerHandshakeActor.MessageTypes.RequestStartHandover));
						clientSide.tell(MockServerHandshakeActor.MessageTypes.OkResponseStartHandover, getRef());
						break;
					case EXECUTE: // now we tell to complete playing the local FU
						clientSide.tell(MessageTypes.Complete, getRef());
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
		
	
	
	private void logEvent(ClientSide event) {
		logger.info(event.toString());
	}
	
	private void logMsg(MockServerHandshakeActor.MessageTypes event) {
		logger.info(event.toString());
	}
	
}
