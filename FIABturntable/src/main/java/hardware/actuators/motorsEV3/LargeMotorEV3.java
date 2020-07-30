package hardware.actuators.motorsEV3;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import hardware.actuators.Motor;
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
        largeRegulatedMotor.resetTachoCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forward() {
        super.forward();
        largeRegulatedMotor.hold();
        //largeRegulatedMotor.brake();
        largeRegulatedMotor.forward();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void backward() {
        super.backward();
        largeRegulatedMotor.hold();
        //largeRegulatedMotor.brake();
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
        largeRegulatedMotor.rotateTo(angle, true);
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
        largeRegulatedMotor.stop();
        largeRegulatedMotor.hold();
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
        largeRegulatedMotor.resetTachoCount();
        largeRegulatedMotor.hold();
    }
}
