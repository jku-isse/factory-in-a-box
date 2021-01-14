package main.java.fiab.core.capabilities.wiring;

public class WiringInfo {
	public WiringInfo() {
		super();
	}

	String localCapabilityId;
	String remoteCapabilityId;
	String remoteEndpointURL;
	String remoteNodeId;
	String remoteRole;
	public String getLocalCapabilityId() {
		return localCapabilityId;
	}
	public void setLocalCapabilityId(String localCapabilityId) {
		this.localCapabilityId = localCapabilityId;
	}
	public String getRemoteCapabilityId() {
		return remoteCapabilityId;
	}
	public void setRemoteCapabilityId(String remoteCapabilityId) {
		this.remoteCapabilityId = remoteCapabilityId;
	}
	public String getRemoteEndpointURL() {
		return remoteEndpointURL;
	}
	public void setRemoteEndpointURL(String remoteEndpointURL) {
		this.remoteEndpointURL = remoteEndpointURL;
	}
	public String getRemoteNodeId() {
		return remoteNodeId;
	}
	public void setRemoteNodeId(String remoteNodeId) {
		this.remoteNodeId = remoteNodeId;
	}
	public String getRemoteRole() {
		return remoteRole;
	}
	public void setRemoteRole(String remoteRole) {
		this.remoteRole = remoteRole;
	}
	
	public WiringInfo(String localCapabilityId, String remoteCapabilityId, String remoteEndpointURL,
			String remoteNodeId, String remoteRole) {
		super();
		this.localCapabilityId = localCapabilityId;
		this.remoteCapabilityId = remoteCapabilityId;
		this.remoteEndpointURL = remoteEndpointURL;
		this.remoteNodeId = remoteNodeId;
		this.remoteRole = remoteRole;
	}
	
	
}