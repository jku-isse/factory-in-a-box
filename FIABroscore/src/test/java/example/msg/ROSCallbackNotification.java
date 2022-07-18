package example.msg;

public class ROSCallbackNotification {

    private final boolean ok;


    public ROSCallbackNotification(boolean ok) {
        this.ok = ok;
    }

    public boolean getOk(){
        return ok;
    }

}
