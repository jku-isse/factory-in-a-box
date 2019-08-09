package motorSensorTest;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.utility.Delay;

/**
 * Class to test out the EV3 Motors
 * To test the motor, connect it to the MotorPort.B on the brick and choose this as your main class
 * Do not connect any gears to the motor
 */
public class MotorTestApplication {

    public static void main(String[] args) {
        EV3LargeRegulatedMotor motor = new EV3LargeRegulatedMotor(MotorPort.B);
        motor.brake();
        motor.setSpeed(50);
        motor.forward();
        Delay.msDelay(2000);
        motor.stop();
        motor.brake();
        motor.setSpeed(100);
        motor.forward();
        Delay.msDelay(2000);
        motor.stop();
        motor.brake();
        motor.setSpeed(175);
        motor.forward();
        Delay.msDelay(2000);
        motor.stop();
        motor.brake();
        motor.setSpeed(255);
        motor.forward();
        Delay.msDelay(2000);
        motor.stop();
    }
}
