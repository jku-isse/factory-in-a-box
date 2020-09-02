package fiab.capabilityTool.gui.msg;

public class ConnectionRequest {

    private final String url;

    public ConnectionRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
