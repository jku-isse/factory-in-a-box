package fiab.opcua.hardwaremock.iostation;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.opcua.hardwaremock.BaseOpcUaServer;

public class StartupOPCUAIOStationMocks {
	static final String NAMESPACE_URI = "urn:factory-in-a-box";

	public static void main(String[] args) throws Exception {
		startupIOatPos34and37onSingleTurntable();
	}
	
	private static void startupIOatPos34and37onSingleTurntable()  throws Exception {
		ActorSystem system = ActorSystem.create("ROOT_SYSTEM");		
		ActorRef actor1 = system.actorOf(OPCUAMockIOStationWrapper.props( true, true));
		ActorRef actor2 = system.actorOf(OPCUAMockIOStationWrapper.props( false, true));
		//ActorRef actor2 = system.actorOf(MockOutputStationServerHandshakeActor.props());
		BaseOpcUaServer server1 = new BaseOpcUaServer(0, "InputStation");
		OPCUAInputStationMock ism1 = new OPCUAInputStationMock(server1.getServer(), NAMESPACE_URI, "InputStation", actor1, IOStationCapability.INPUTSTATION_CAPABILITY_URI);
		BaseOpcUaServer server2 = new BaseOpcUaServer(7, "OutputStation");
		OPCUAInputStationMock ism2 = new OPCUAInputStationMock(server2.getServer(), NAMESPACE_URI, "OutputStation", actor2, IOStationCapability.OUTPUTSTATION_CAPABILITY_URI);
		// differentiate in/out
		Thread s1 = new Thread(ism1);
		Thread s2 = new Thread(ism2);
		s1.start();
		s2.start();
	}
	
	private static void startupIOatPos34and35acrossTwoTurntables() throws Exception{
		ActorSystem system = ActorSystem.create("ROOT_SYSTEM");		
		ActorRef actor1 = system.actorOf(OPCUAMockIOStationWrapper.props( true, true));
		ActorRef actor2 = system.actorOf(OPCUAMockIOStationWrapper.props( false, true));
		//ActorRef actor2 = system.actorOf(MockOutputStationServerHandshakeActor.props());
		BaseOpcUaServer server1 = new BaseOpcUaServer(0, "InputStation");
		OPCUAInputStationMock ism1 = new OPCUAInputStationMock(server1.getServer(), NAMESPACE_URI, "InputStation", actor1, IOStationCapability.INPUTSTATION_CAPABILITY_URI);
		BaseOpcUaServer server2 = new BaseOpcUaServer(1, "OutputStation");
		OPCUAInputStationMock ism2 = new OPCUAInputStationMock(server2.getServer(), NAMESPACE_URI, "OutputStation", actor2, IOStationCapability.OUTPUTSTATION_CAPABILITY_URI);
		// differentiate in/out
		Thread s1 = new Thread(ism1);
		Thread s2 = new Thread(ism2);
		s1.start();
		s2.start();
	}
}
