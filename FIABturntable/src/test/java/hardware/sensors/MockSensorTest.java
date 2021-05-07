package hardware.sensors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MockSensorTest {

    MockSensor mockSensor;

    @BeforeEach
    void buildTest(){
        mockSensor = new MockSensor();
    }

    @Test
    void testNoInputAtStart(){
        assertFalse(mockSensor.hasDetectedInput());
    }

    @Test
    void testSetDetected(){
        mockSensor.setDetectedInput(true);
        assertTrue(mockSensor.hasDetectedInput());
        mockSensor.setDetectedInput(false);
        assertFalse(mockSensor.hasDetectedInput());
    }


}
