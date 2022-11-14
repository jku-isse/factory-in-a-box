package hardware;

import hardware.actuators.Motor;
import hardware.sensors.Sensor;

public abstract class TurningHardware {

    protected Motor turningMotor;
    protected Sensor sensorHoming;

    public Motor getTurningMotor() {
        return turningMotor;
    }

    public Sensor getSensorHoming() {
        return sensorHoming;
    }

    public void stopTurningMotor() {
        turningMotor.stop();
    }

    public boolean isInHomePosition() {
        return sensorHoming.hasDetectedInput();
    }

    public void startMotorBackward(){
        turningMotor.backward();
    }

    public void startMotorForward(){
        turningMotor.forward();
    }

    public void rotateMotorToAngle(int angle){
        turningMotor.rotateTo(angle);
    }

    public int getMotorAngle(){
        return turningMotor.getRotationAngle();
    }

    public void resetMotorAngleToZero(){
        turningMotor.resetTachoCount();
    }
}
