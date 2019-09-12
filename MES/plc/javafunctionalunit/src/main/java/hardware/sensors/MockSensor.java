package hardware.sensors;

import java.util.Random;

public class MockSensor extends Sensor {

    private Random random;

    public MockSensor(){
        random = new Random();
    }
    @Override
    public boolean detectedInput() {
        return random.nextBoolean();
    }
}
