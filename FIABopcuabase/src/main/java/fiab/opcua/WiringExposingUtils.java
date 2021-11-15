package fiab.opcua;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.opcua.server.OPCUABase;
import fiab.opcua.wiring.WiringNodes;

public class WiringExposingUtils {

	public static final String WIRING_INFO_BROWSENAME = "WiringInformation";

	public static WiringNodes createWiringInfoFolder(OPCUABase base, UaFolderNode handshakeNode, String path) {
		UaFolderNode wiringInfoFolder = base.generateFolder(handshakeNode, path,
				OPCUACapabilitiesAndWiringInfoBrowsenames.WIRING_INFO);
		String wiringPath = path + "/" + WIRING_INFO_BROWSENAME;
		WiringNodes wn = new WiringNodes();
		wn.setRemoteCapabilityId(base.generateStringVariableNode(
				wiringInfoFolder, wiringPath, OPCUACapabilitiesAndWiringInfoBrowsenames.REMOTE_CAPABILITYID, ""));
		wn.setRemoteEndpointURI(base.generateStringVariableNode(
				wiringInfoFolder, wiringPath, OPCUACapabilitiesAndWiringInfoBrowsenames.REMOTE_ENDPOINT, ""));
		wn.setRemoteNodeId(base.generateStringVariableNode(
				wiringInfoFolder, wiringPath, OPCUACapabilitiesAndWiringInfoBrowsenames.REMOTE_NODEID, ""));
		wn.setRemoteRole(base.generateStringVariableNode(
				wiringInfoFolder, wiringPath, OPCUACapabilitiesAndWiringInfoBrowsenames.REMOTE_ROLE, ""));
		return wn;
	}

	public static void updateWiring(WiringNodes wiringNodes, String capId, String endpoint, String nodeid, String role) {
		wiringNodes.getRemoteCapabilityId().setValue(new DataValue(new Variant(capId)));
		wiringNodes.getRemoteEndpointURI().setValue(new DataValue(new Variant(endpoint)));
		wiringNodes.getRemoteNodeId().setValue(new DataValue(new Variant(nodeid)));
		wiringNodes.getRemoteRole().setValue(new DataValue(new Variant(role)));
	}
}
