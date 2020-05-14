package fiab.opcua.wiring;

import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;

public class WiringNodes {
	UaVariableNode remoteCapabilityId;
	UaVariableNode remoteEndpointURI;
	UaVariableNode remoteNodeId;
	UaVariableNode remoteRole;
	
	
	
	public UaVariableNode getRemoteCapabilityId() {
		return remoteCapabilityId;
	}
	public void setRemoteCapabilityId(UaVariableNode remoteCapabilityId) {
		this.remoteCapabilityId = remoteCapabilityId;
	}
	public UaVariableNode getRemoteEndpointURI() {
		return remoteEndpointURI;
	}
	public void setRemoteEndpointURI(UaVariableNode remoteEndpointURI) {
		this.remoteEndpointURI = remoteEndpointURI;
	}
	public UaVariableNode getRemoteNodeId() {
		return remoteNodeId;
	}
	public void setRemoteNodeId(UaVariableNode remoteNodeId) {
		this.remoteNodeId = remoteNodeId;
	}
	public UaVariableNode getRemoteRole() {
		return remoteRole;
	}
	public void setRemoteRole(UaVariableNode remoteRole) {
		this.remoteRole = remoteRole;
	}
}
