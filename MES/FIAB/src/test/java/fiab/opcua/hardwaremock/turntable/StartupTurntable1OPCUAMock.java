package fiab.opcua.hardwaremock.turntable;

import akka.actor.ActorSystem;

public class StartupTurntable1OPCUAMock {	

	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA");
		//system.actorOf(OPCUATurntableRootActor.props("Turntable1", true));
		system.actorOf(OPCUATurntableRootActor.props("Turntable1", false));
	}
	
//	public static void main(String[] args) throws Exception {
//		writeExampleWiring();
//	}
//	
//	
//	public static void writeExampleWiring() {
//		HashMap<String,WiringInfo> infos = new HashMap<>();
//		infos.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT, new WiringInfo(WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT, "DefaultHandshake", "opc.tcp://localhost:4840/", "ns=1;s=Capability1", "RemoteRole1" ));
//		infos.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT, new WiringInfo(WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT, "defaultHandshake", "opc.tcp://localhost:4841/", "ns=1;s=Capability1", "RemoteRole1" ));
//		WiringUtils.writeWiringInfoToFileSystem(infos);
//	}
	
}
