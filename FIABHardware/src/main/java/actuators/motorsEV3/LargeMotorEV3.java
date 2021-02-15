package actuators.motorsEV3;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import actuators.Motor;
import lejos.hardware.port.Port;
import lejos.utility.Delay;

/**
 * This class is an implementation for the EV3 Large Motor. It provides all functionality any motor
 * should be able to provide.
 */
public class LargeMotorEV3 extends Motor {

    private final EV3LargeRegulatedMotor largeRegulatedMotor;

    public LargeMotorEV3(Port motorPort) {
        super();
        this.largeRegulatedMotor = new EV3LargeRegulatedMotor(motorPort);
        this.largeRegulatedMotor.setAcceleration(5000);     //6000 is default value, but this may make rotation smoother
        //largeRegulatedMotor.resetTachoCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forward() {
        super.forward();
        //largeRegulatedMotor.hold();
        largeRegulatedMotor.brake();
        largeRegulatedMotor.forward();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void backward() {
        super.backward();
        //largeRegulatedMotor.hold();  //Should only be used when turning to a position setpoint (position_sp)
        largeRegulatedMotor.brake();   //Use this for run-forever/run-direct
        largeRegulatedMotor.backward();
    }

    @Override
    public void rotate(int angle) {
        super.rotate(angle);
        largeRegulatedMotor.hold();
        largeRegulatedMotor.rotate(angle);
    }

    @Override
    public void rotateTo(int angle) {
        super.rotateTo(angle);
        largeRegulatedMotor.hold();
        largeRegulatedMotor.rotateTo(angle, true);  //immediateReturn prevents blocking
    }

    @Override
    public int getRotationAngle() {
        return (int) largeRegulatedMotor.getPosition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        super.stop();
        largeRegulatedMotor.hold();
        largeRegulatedMotor.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSpeed(int speed) {
        largeRegulatedMotor.setSpeed(speed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitMs(long msDelay) {
        Delay.msDelay(msDelay);
    }

    @Override
    public void resetTachoCount() {
        largeRegulatedMotor.hold();
        largeRegulatedMotor.resetTachoCount();
    }
}
