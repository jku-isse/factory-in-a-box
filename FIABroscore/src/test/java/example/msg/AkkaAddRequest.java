package example.msg;

/**
 * This message will be used to tell the ROSClientActor to perform a ROS call to add two ints
 */
public class AkkaAddRequest {

    private final int a;
    private final int b;

    public AkkaAddRequest(int a, int b){
        this.a = a;
        this.b = b;
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }
}
