package fiab.core.capabilities.wiring;

import java.util.Objects;

public class WiringInfo {

    private String localCapabilityId;
    private String remoteCapabilityId;
    private String remoteEndpointURL;
    private String remoteNodeId;
    private String remoteRole;

    /**
     * @param localCapabilityId  capabilityId of the handshake that uses this wiring info e.g. "NORTH_CLIENT"
     * @param remoteCapabilityId remote capabilityId of the handshake e.g. "DefaultHandshakeServerSide"
     * @param remoteEndpointURL  remote endpoint URL e.g. "opc.tcp://192.168.0.34:4840"
     * @param remoteNodeId       remote nodeId of the handshake capability folder
     * @param remoteRole         remote role e.g. "RemoteRole1"
     */
    public WiringInfo(String localCapabilityId, String remoteCapabilityId, String remoteEndpointURL,
                      String remoteNodeId, String remoteRole) {
        this.localCapabilityId = localCapabilityId;
        this.remoteCapabilityId = remoteCapabilityId;
        this.remoteEndpointURL = remoteEndpointURL;
        this.remoteNodeId = remoteNodeId;
        this.remoteRole = remoteRole;
    }

    /**
     * This is used by the jackson databind api
     */
    public WiringInfo(){
        this.localCapabilityId = "";
        this.remoteCapabilityId = "";
        this.remoteEndpointURL = "";
        this.remoteNodeId = "";
        this.remoteRole = "";
    }

    public void setLocalCapabilityId(String localCapabilityId) {
        this.localCapabilityId = localCapabilityId;
    }

    public void setRemoteCapabilityId(String remoteCapabilityId) {
        this.remoteCapabilityId = remoteCapabilityId;
    }

    public void setRemoteEndpointURL(String remoteEndpointURL) {
        this.remoteEndpointURL = remoteEndpointURL;
    }

    public void setRemoteNodeId(String remoteNodeId) {
        this.remoteNodeId = remoteNodeId;
    }

    public void setRemoteRole(String remoteRole) {
        this.remoteRole = remoteRole;
    }

    /**
     * @return capabilityId of the handshake that uses this wiring info e.g. "NORTH_CLIENT"
     */
    public String getLocalCapabilityId() {
        return localCapabilityId;
    }

    /**
     * @return remote capabilityId of the handshake e.g. "DefaultHandshakeServerSide"
     */
    public String getRemoteCapabilityId() {
        return remoteCapabilityId;
    }

    /**
     * @return remote endpoint URL e.g. "opc.tcp://192.168.0.34:4840"
     */
    public String getRemoteEndpointURL() {
        return remoteEndpointURL;
    }

    /**
     * @return remote nodeId of the handshake capability folder
     */
    public String getRemoteNodeId() {
        return remoteNodeId;
    }

    /**
     * @return remote role e.g. "RemoteRole1"
     */
    public String getRemoteRole() {
        return remoteRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WiringInfo that = (WiringInfo) o;
        return Objects.equals(localCapabilityId, that.localCapabilityId) && Objects.equals(remoteCapabilityId, that.remoteCapabilityId) && Objects.equals(remoteEndpointURL, that.remoteEndpointURL) && Objects.equals(remoteNodeId, that.remoteNodeId) && Objects.equals(remoteRole, that.remoteRole);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localCapabilityId, remoteCapabilityId, remoteEndpointURL, remoteNodeId, remoteRole);
    }

    @Override
    public String toString() {
        return "WiringInfo{" +
                "localCapabilityId='" + localCapabilityId + '\'' +
                ", remoteCapabilityId='" + remoteCapabilityId + '\'' +
                ", remoteEndpointURL='" + remoteEndpointURL + '\'' +
                ", remoteNodeId='" + remoteNodeId + '\'' +
                ", remoteRole='" + remoteRole + '\'' +
                '}';
    }
}