package fiab.core.capabilities.wiring;

public class WiringInfoBuilder {

    private String localCapabilityId;
    private String remoteCapabilityId;
    private String remoteEndpointURL;
    private String remoteNodeId;
    private String remoteRole;

    public WiringInfoBuilder(){
        this.localCapabilityId = "";
        this.remoteCapabilityId = "";
        this.remoteEndpointURL = "";
        this.remoteNodeId = "";
        this.remoteRole = "";
    }

    public WiringInfoBuilder setLocalCapabilityId(String localCapabilityId){
        this.localCapabilityId = localCapabilityId;
        return this;
    }

    public WiringInfoBuilder setRemoteCapabilityId(String remoteCapabilityId){
        this.remoteCapabilityId = remoteCapabilityId;
        return this;
    }

    public WiringInfoBuilder setRemoteEndpointURL(String remoteEndpointURL){
        this.remoteEndpointURL = remoteEndpointURL;
        return this;
    }

    public WiringInfoBuilder setRemoteNodeId(String remoteNodeId){
        this.remoteNodeId = remoteNodeId;
        return this;
    }

    public WiringInfoBuilder setRemoteRole(String remoteRole){
        this.remoteRole = remoteRole;
        return this;
    }

    public WiringInfo build(){
        return new WiringInfo(this.localCapabilityId,
                this.remoteCapabilityId, this.remoteEndpointURL, this.remoteNodeId, remoteRole);
    }

}
