package fiab.handshake.client.opcua;

import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;

public class UaWiringInfoNodes {
    private UaVariableNode localCapabilityIdNode;
    private UaVariableNode remoteCapabilityIdNode;
    private UaVariableNode remoteCapabilityURLNode;
    private UaVariableNode remoteNodeIdNode;
    private UaVariableNode remoteRoleNode;

    public UaVariableNode getLocalCapabilityIdNode() {
        return localCapabilityIdNode;
    }

    public void setLocalCapabilityIdNode(UaVariableNode localCapabilityIdNode) {
        this.localCapabilityIdNode = localCapabilityIdNode;
    }

    public UaVariableNode getRemoteCapabilityIdNode() {
        return remoteCapabilityIdNode;
    }

    public void setRemoteCapabilityIdNode(UaVariableNode remoteCapabilityIdNode) {
        this.remoteCapabilityIdNode = remoteCapabilityIdNode;
    }

    public UaVariableNode getRemoteCapabilityURLNode() {
        return remoteCapabilityURLNode;
    }

    public void setRemoteCapabilityURLNode(UaVariableNode remoteCapabilityURLNode) {
        this.remoteCapabilityURLNode = remoteCapabilityURLNode;
    }

    public UaVariableNode getRemoteNodeIdNode() {
        return remoteNodeIdNode;
    }

    public void setRemoteNodeIdNode(UaVariableNode remoteNodeIdNode) {
        this.remoteNodeIdNode = remoteNodeIdNode;
    }

    public UaVariableNode getRemoteRoleNode() {
        return remoteRoleNode;
    }

    public void setRemoteRoleNode(UaVariableNode remoteRoleNode) {
        this.remoteRoleNode = remoteRoleNode;
    }
}
