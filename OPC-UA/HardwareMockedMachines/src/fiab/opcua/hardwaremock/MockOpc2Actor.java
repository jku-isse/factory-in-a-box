package fiab.opcua.hardwaremock;

import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.mes.mockactors.iostation.MockInputStationServerHandshakeActor;
import fiab.opcua.hardwaremock.methods.StopMethod;

public class MockOpc2Actor {
	static final String NAMESPACE_URI = "urn:factory-in-a-box";

	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create("ROOT_SYSTEM");
		ActorRef actor = system.actorOf(MockInputStationServerHandshakeActor.props());
		StopMethod method = new StopMethod(actor);
		
		BaseOpcUaServer server1 = new BaseOpcUaServer(0);
		InputStationMock ism1 = new InputStationMock(server1.getServer(), NAMESPACE_URI, method);
		BaseOpcUaServer server2 = new BaseOpcUaServer(1);
		InputStationMock ism2 = new InputStationMock(server2.getServer(), NAMESPACE_URI, method);
		// differentiate in/out
		Thread s1 = new Thread(ism1);
		Thread s2 = new Thread(ism2);
		s1.start();
		s2.start();
	}
}
