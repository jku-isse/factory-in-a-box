package fiab.handshake.fu;

import akka.actor.ActorRef;
import fiab.core.capabilities.wiring.WiringInfo;

public interface HandshakeFU {

	ActorRef getFUActor();

	void provideWiringInfo(WiringInfo info) throws Exception;

}