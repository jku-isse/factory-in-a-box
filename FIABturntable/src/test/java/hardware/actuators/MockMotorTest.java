package hardware.actuators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MockMotorTest {

    private MockMotor mockMotor;

    @BeforeEach
    void buildTest(){
        mockMotor = new MockMotor(100);
    }

    @Test
    void testSetSpeed(){
        mockMotor.setSpeed(200);
        assertEquals(mockMotor.getMotorSpeed(), 200);
    }

    @Test
    void testForward(){
        mockMotor.forward();
        assertTrue(mockMotor.isRunning());
    }

    @Test
    void testBackward(){
        mockMotor.backward();
        assertTrue(mockMotor.isRunning());
    }

    @Test
    void testStoppedAtStart(){
        assertFalse(mockMotor.isRunning());
    }

    @Test
    void testStopFromForward(){
        mockMotor.forward();
        assertTrue(mockMotor.isRunning());
        mockMotor.stop();
        assertFalse(mockMotor.isRunning());
    }

    @Test
    void testStopFromBackward(){
        mockMotor.backward();
        assertTrue(mockMotor.isRunning());
        mockMotor.stop();
        assertFalse(mockMotor.isRunning());
    }
}
