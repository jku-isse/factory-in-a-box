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
import fiab.core.capabilities.handshake.HandshakeCapability.ClientSide;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSide;
import fiab.core.capabilities.handshake.IOStationCapability;
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
				boolean doAutoComplete = false;
				ActorRef serverSide = system.actorOf(MockServerHandshakeActor.props(getRef(), doAutoComplete).withDispatcher(CallingThreadDispatcher.Id()), "ServerSide"); 
				ActorRef clientSide = system.actorOf(MockClientHandshakeActor.props(getRef(), serverSide).withDispatcher(CallingThreadDispatcher.Id()), "ClientSide"); 
				
				boolean serverDone = false;
				boolean clientDone = false;
				clientSide.tell(IOStationCapability.ClientMessageTypes.Reset, getRef());
				serverSide.tell(IOStationCapability.ServerMessageTypes.Reset, getRef());
				while (!(serverDone && clientDone)) {
					Object msg = expectMsgAnyClassOf(Duration.ofSeconds(3600), ClientSide.class, ServerSide.class);
					logEvent(msg, getLastSender());
					if (msg.equals(ClientSide.IDLE)) {
						clientSide.tell(IOStationCapability.ClientMessageTypes.Start, getRef());
					}
					if (msg.equals(ServerSide.EXECUTE)) {
						serverSide.tell(IOStationCapability.ServerMessageTypes.Complete, getRef());
					}
					if (msg.equals(ServerSide.COMPLETE)) {
						serverDone = true;
					}
					if (msg.equals(ClientSide.COMPLETED)) {
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
