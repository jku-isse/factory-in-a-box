package fiab.mes.opcuawrappers;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.Test;

import fiab.mes.machine.actor.iostation.wrapper.IOStationOPCUAWrapper;

class TestIOStationOPCUAWrapper {

	@Test
	void testReset() throws Exception {
		// assume OPCUA server (mock or otherwise is started
		NodeId capabilitImpl = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU");
		NodeId resetMethod = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/RESET");
		NodeId stopMethod = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/STOP");
		NodeId stateVar = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/STATE");
		OpcUaClient client = new OPCUAUtils().createClient("opc.tcp://localhost:4840/milo");
		client.connect().get();
		IOStationOPCUAWrapper wrapper = new IOStationOPCUAWrapper(null, client, capabilitImpl, stopMethod, resetMethod, stateVar);
		wrapper.reset();
	}

}
