package hardware;

import hardware.actuators.Motor;
import hardware.sensors.Sensor;

public abstract class ConveyorHardware {

    protected Motor conveyorMotor;
    protected Sensor loadingSensor;
    protected Sensor unloadingSensor;

    public Motor getConveyorMotor() {
        return conveyorMotor;
    }

    public Sensor getLoadingSensor() {
        return loadingSensor;
    }

    public Sensor getUnloadingSensor() {
        return unloadingSensor;
    }

    public boolean isLoadingSensorDetectingPallet(){
        return loadingSensor.hasDetectedInput();
    }

    public boolean isUnloadingSensorDetectingPallet(){
        return unloadingSensor.hasDetectedInput();
    }

    public void startMotorForLoading(){
        conveyorMotor.backward();
    }

    public void startMotorForUnloading(){
        conveyorMotor.forward();
    }

    public void stopConveyorMotor(){
        conveyorMotor.stop();
    }
}
