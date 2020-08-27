package fiab.capabilityTool.gui.msg;

public class WriteRequest {

    private final String data;

    public WriteRequest(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
