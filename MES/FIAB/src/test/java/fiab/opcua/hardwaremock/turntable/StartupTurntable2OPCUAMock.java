package fiab.opcua.hardwaremock.turntable;

import akka.actor.ActorSystem;

public class StartupTurntable2OPCUAMock {	

	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create("ROOT_SYSTEM_TURNTABLE2_OPCUA");
		system.actorOf(OPCUATurntableRootActor.props("Turntable2"));
	}
	
}
