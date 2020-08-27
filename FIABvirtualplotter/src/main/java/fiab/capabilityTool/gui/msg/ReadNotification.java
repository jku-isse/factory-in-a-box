package fiab.capabilityTool.gui.msg;

public class ReadNotification {

    private final String value;

    public ReadNotification(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
