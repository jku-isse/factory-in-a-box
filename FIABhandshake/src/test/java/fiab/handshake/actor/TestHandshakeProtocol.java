package fiab.handshake.actor;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.CallingThreadDispatcher;
import akka.testkit.javadsl.TestKit;
import brave.ScopedSpan;
import brave.Tracing;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerMessageTypes;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.tracing.TestTracingUtil;
import fiab.handshake.actor.messages.HSClientMessage;
import fiab.handshake.actor.messages.HSClientSideStateMessage;
import fiab.handshake.actor.messages.HSServerMessage;
import fiab.handshake.actor.messages.HSServerSideStateMessage;
import fiab.tracing.actor.messages.ExtensibleMessage;
import fiab.tracing.impl.zipkin.ZipkinTracing;
import fiab.tracing.impl.zipkin.ZipkinUtil;

public class TestHandshakeProtocol {

	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "routes";

	private static final Logger logger = LoggerFactory.getLogger(TestHandshakeProtocol.class);

	@BeforeAll
	static void setUpBeforeClass() throws Exception {

		system = ActorSystem.create(ROOT_SYSTEM);
		system.registerExtension(TestTracingUtil.getTracingExtension());

	}

//	

	@AfterAll
	public static void teardown() {
		TestKit.shutdownActorSystem(system);
		system = null;
	}

	@Test
	void testProtocol() {
		new TestKit(system) {
			{
				boolean doAutoComplete = false;
				ActorRef serverSide = system.actorOf(ServerSideHandshakeActor.props(getRef(), doAutoComplete)
						.withDispatcher(CallingThreadDispatcher.Id()), "ServerSide");
				ActorRef clientSide = system.actorOf(
						ClientHandshakeActor.props(getRef(), serverSide).withDispatcher(CallingThreadDispatcher.Id()),
						"ClientSide");

				boolean serverDone = false;
				boolean clientDone = false;
				clientSide.tell(HandshakeCapability.ClientMessageTypes.Reset, getRef());
				serverSide.tell(HandshakeCapability.ServerMessageTypes.Reset, getRef());
				while (!(serverDone && clientDone)) {

					Object msg = expectMsgAnyClassOf(Duration.ofSeconds(3600), HSClientSideStateMessage.class,
							HSServerSideStateMessage.class);
					ExtensibleMessage<Object> exMsg = null;
					if (msg instanceof ExtensibleMessage<?>)
						exMsg = (ExtensibleMessage<Object>) msg;

					logEvent(msg, getLastSender());
					String header = ZipkinUtil.createXB3ScopeHeader(createScopedSpan());
					if (exMsg.getBody().equals(ClientSideStates.IDLE)) {
						HSClientMessage scopedMsg = new HSClientMessage(header,
								HandshakeCapability.ClientMessageTypes.Start);
						clientSide.tell(scopedMsg, getRef());
					}
					if (exMsg.getBody().equals(ServerSideStates.EXECUTE)) {
						serverSide.tell(new HSServerMessage(header, ServerMessageTypes.Complete), getRef());
					}
					if (exMsg.getBody().equals(ServerSideStates.COMPLETE)) {
						serverDone = true;
					}
					if (exMsg.getBody().equals(ClientSideStates.COMPLETED)) {
						clientDone = true;
					}
				}
			}
		};
	}

	private ScopedSpan createScopedSpan() {
		return Tracing.newBuilder().localServiceName("Order1").addSpanHandler(ZipkinTracing.getHandler()).build()
				.tracer().startScopedSpan("Order1");
	}

	private void logEvent(Object event, ActorRef sender) {
		logger.info(sender.toString() + ": " + event.toString());
	}

}
