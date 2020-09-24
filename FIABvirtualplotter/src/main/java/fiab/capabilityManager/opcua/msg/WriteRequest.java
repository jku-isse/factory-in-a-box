package fiab.capabilityManager.opcua.msg;

public class WriteRequest {

    private final String data;

    public WriteRequest(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
