package fiab.mes.mockactors;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.CallingThreadDispatcher;
import akka.testkit.javadsl.TestKit;
import fiab.mes.handshake.HandshakeProtocol.ClientSide;
import fiab.mes.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.mockactors.MockClientHandshakeActor.MessageTypes;
import fiab.mes.order.OrderProcess;

public class TestHandshakeProtocol { 

	protected static ActorSystem system;	
	public static String ROOT_SYSTEM = "routes";
	protected OrderProcess op;
	
	private static final Logger logger = LoggerFactory.getLogger(TestHandshakeProtocol.class);
	
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
	void testProtocol() {		
		new TestKit(system) { 
			{
				ActorRef serverSide = system.actorOf(MockServerHandshakeActor.props(getRef()).withDispatcher(CallingThreadDispatcher.Id()), "ServerSide"); 
				ActorRef clientSide = system.actorOf(MockClientHandshakeActor.props(getRef(), serverSide).withDispatcher(CallingThreadDispatcher.Id()), "ClientSide"); 
				
				boolean serverDone = false;
				boolean clientDone = false;
				clientSide.tell(MockClientHandshakeActor.MessageTypes.Reset, getRef());
				serverSide.tell(MockServerHandshakeActor.MessageTypes.Reset, getRef());
				while (!(serverDone && clientDone)) {
					Object msg = expectMsgAnyClassOf(Duration.ofSeconds(3600), ClientSide.class, ServerSide.class);
					logEvent(msg, getLastSender());
					if (msg.equals(ClientSide.Idle)) {
						clientSide.tell(MockClientHandshakeActor.MessageTypes.Start, getRef());
					}
					if (msg.equals(ServerSide.Execute)) {
						serverSide.tell(MockServerHandshakeActor.MessageTypes.Complete, getRef());
					}
					if (msg.equals(ServerSide.Stopped)) {
						serverDone = true;
					}
					if (msg.equals(ClientSide.Stopped)) {
						clientDone = true;
					}
				}				
			}	
		};
	}
		
	
	
	private void logEvent(Object event, ActorRef sender) {
		logger.info(sender.toString() +": "+event.toString());
	}
	

	
}
