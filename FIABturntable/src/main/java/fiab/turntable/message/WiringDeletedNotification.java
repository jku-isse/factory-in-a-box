package fiab.turntable.message;

public class WiringDeletedNotification {

    private final boolean success;

    public WiringDeletedNotification() {
        this(true);
    }

    public WiringDeletedNotification(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
