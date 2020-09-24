package fiab.capabilityManager.opcua.msg;

public class ClientReadyNotification {

    private final String endpointUrl;

    public ClientReadyNotification(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
