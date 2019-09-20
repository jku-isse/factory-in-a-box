package hardware.actuators.motorsEV3;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import hardware.actuators.Motor;
import lejos.hardware.port.Port;
import lejos.utility.Delay;

public class LargeMotorEV3 extends Motor {

    private final EV3LargeRegulatedMotor largeRegulatedMotor;

    public LargeMotorEV3(Port motorPort){
        this.largeRegulatedMotor = new EV3LargeRegulatedMotor(motorPort);
    }

    @Override
    public void forward() {
        largeRegulatedMotor.brake();
        largeRegulatedMotor.forward();
    }

    @Override
    public void backward() {
        largeRegulatedMotor.brake();
        largeRegulatedMotor.backward();
    }

    @Override
    public void stop() {
        largeRegulatedMotor.stop();
    }

    @Override
    public void setSpeed(int speed) {
        largeRegulatedMotor.setSpeed(speed);
    }

    @Override
    public void waitMs(int msDelay){
        Delay.msDelay(msDelay);
    }

}
