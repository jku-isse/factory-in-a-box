package hardware.actuators.motorsEV3;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import hardware.actuators.Motor;
import lejos.hardware.port.Port;
import lejos.utility.Delay;

/**
 * This class is an implementation for the EV3 Medium Motor. It provides all functionality any motor
 * should be able to provide.
 */
public class MediumMotorEV3 extends Motor {

    private final EV3MediumRegulatedMotor mediumRegulatedMotor;

    public MediumMotorEV3(Port motorPort) {
        super();
        this.mediumRegulatedMotor = new EV3MediumRegulatedMotor(motorPort);
        mediumRegulatedMotor.resetTachoCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forward() {
        super.forward();
        mediumRegulatedMotor.brake();
        mediumRegulatedMotor.forward();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void backward() {
        super.backward();
        mediumRegulatedMotor.brake();
        mediumRegulatedMotor.backward();
    }

    @Override
    public void rotate(int angle){
        super.rotate(angle);
        mediumRegulatedMotor.rotate(angle);
    }

    @Override
    public void rotateTo(int angle){
        super.rotateTo(angle);
        mediumRegulatedMotor.rotateTo(angle, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        super.stop();
        mediumRegulatedMotor.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSpeed(int speed) {
        mediumRegulatedMotor.setSpeed(speed);
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
        mediumRegulatedMotor.resetTachoCount();
    }
}
