package hardware.motors.motorsEV3;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import ev3dev.hardware.EV3DevMotorDevice;
import hardware.motors.Motor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.utility.Delay;

public class MediumMotorEV3 extends Motor {

    private final EV3MediumRegulatedMotor mediumRegulatedMotor;

    public MediumMotorEV3(Port motorPort){
        this.mediumRegulatedMotor = new EV3MediumRegulatedMotor(motorPort);
    }

    @Override
    public void forward() {
        mediumRegulatedMotor.brake();
        mediumRegulatedMotor.forward();
    }

    @Override
    public void backward() {
        mediumRegulatedMotor.brake();
        mediumRegulatedMotor.backward();
    }

    @Override
    public void stop() {
        mediumRegulatedMotor.stop();
    }

    @Override
    public void setSpeed(int speed) {
        mediumRegulatedMotor.setSpeed(speed);
    }

    @Override
    public void waitMs(int msDelay){
        Delay.msDelay(msDelay);
    }

}
