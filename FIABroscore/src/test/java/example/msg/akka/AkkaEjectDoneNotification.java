package example.msg.akka;

public class AkkaEjectDoneNotification {

    private final boolean success;


    public AkkaEjectDoneNotification(boolean success) {
        this.success = success;
    }

    public boolean getSuccess(){
        return success;
    }
}
