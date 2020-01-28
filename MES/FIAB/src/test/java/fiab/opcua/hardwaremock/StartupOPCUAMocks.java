package fiab.opcua.hardwaremock;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.mockactors.MockServerHandshakeActor.MessageTypes;
import fiab.mes.mockactors.iostation.MockInputStationServerHandshakeActor;
import fiab.mes.mockactors.iostation.MockOutputStationServerHandshakeActor;
import fiab.mes.transport.handshake.HandshakeProtocol;

public class StartupOPCUAMocks {
	static final String NAMESPACE_URI = "urn:factory-in-a-box";

	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create("ROOT_SYSTEM");

		
		ActorRef actor1 = system.actorOf(OPCUAMockIOStationWrapper.props( true, true));
		ActorRef actor2 = system.actorOf(OPCUAMockIOStationWrapper.props( false, true));
		//ActorRef actor2 = system.actorOf(MockOutputStationServerHandshakeActor.props());
		BaseOpcUaServer server1 = new BaseOpcUaServer(0);
		OPCUAInputStationMock ism1 = new OPCUAInputStationMock(server1.getServer(), NAMESPACE_URI, "InputStation", actor1, HandshakeProtocol.INPUTSTATION_CAPABILITY_URI);
		BaseOpcUaServer server2 = new BaseOpcUaServer(1);
		OPCUAInputStationMock ism2 = new OPCUAInputStationMock(server2.getServer(), NAMESPACE_URI, "OutputStation", actor2, HandshakeProtocol.OUTPUTSTATION_CAPABILITY_URI);
		// differentiate in/out
		Thread s1 = new Thread(ism1);
		Thread s2 = new Thread(ism2);
		s1.start();
		s2.start();
	}
}
