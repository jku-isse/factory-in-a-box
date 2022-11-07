import hardware.TurningHardware;
import hardware.mock.TurningMockHardware;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestTurningHAL {

    @Test
    public void testMockTurningHAL(){
        TurningHardware turningHardware = new TurningMockHardware();
        assertFalse(turningHardware.getSensorHoming().hasDetectedInput());
        assertFalse(turningHardware.isInHomePosition());

        turningHardware.startMotorBackward();
        await().until(() -> turningHardware.isInHomePosition());
        turningHardware.stopTurningMotor();

        turningHardware.rotateMotorToAngle(270);
        await().until(() -> turningHardware.getMotorAngle() > 100);
    }
}
