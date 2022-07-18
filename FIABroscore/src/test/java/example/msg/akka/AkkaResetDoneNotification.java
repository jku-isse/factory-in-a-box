package example.msg.akka;

public class AkkaResetDoneNotification {

    private final boolean success;

    public AkkaResetDoneNotification(boolean success) {
        this.success = success;
    }

    public boolean getSuccess(){
        return success;
    }
}
