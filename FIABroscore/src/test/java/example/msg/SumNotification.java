package example.msg;

/**
 * This will be used to notify the parent of the sum
 */
public class SumNotification {

    private final long sum;

    public SumNotification(long sum) {
        this.sum = sum;
    }

    public long getSum() {
        return sum;
    }
}
