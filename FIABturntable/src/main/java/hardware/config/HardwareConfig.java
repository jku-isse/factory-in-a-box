package hardware.config;

import hardware.ConveyorHardware;
import hardware.TurningHardware;
import hardware.actuators.ConveyorMockMotor;
import hardware.actuators.MockMotor;
import hardware.actuators.Motor;
import hardware.actuators.TurningMockMotor;
import hardware.actuators.motorsEV3.LargeMotorEV3;
import hardware.actuators.motorsEV3.MediumMotorEV3;
import hardware.lego.LegoConveyorHardware;
import hardware.lego.LegoTurningHardware;
import hardware.mock.ConveyorMockHardware;
import hardware.mock.TurningMockHardware;
import hardware.sensors.MockSensor;
import hardware.sensors.Sensor;
import hardware.sensors.sensorsEV3.ColorSensorEV3;
import hardware.sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.Touch;

import java.util.Optional;

public class HardwareConfig {
    private static final boolean DEBUG = System.getProperty("os.name").toLowerCase().contains("win");

    protected Motor motorA;
    protected Motor motorB;
    protected Motor motorC;
    protected Motor motorD;

    protected Sensor sensor1;
    protected Sensor sensor2;
    protected Sensor sensor3;
    protected Sensor sensor4;

    private final ConveyorHardware conveyorHardware;
    private final TurningHardware turningHardware;

    public HardwareConfig() {
        if (DEBUG) {
            this.motorA = new ConveyorMockMotor(100, 200);  //Mock ConveyorMotor
            this.motorD = new TurningMockMotor(200);   //Mock TurningMotor

            this.sensor2 = new MockSensor();    //Mock LoadingSensor conveyor
            this.sensor3 = new MockSensor();    //Mock UnloadingSensor conveyor
            this.sensor4 = new MockSensor();    //Mock HomingSensor turning

            this.conveyorHardware = new ConveyorMockHardware(motorA, sensor2, sensor3);
            this.turningHardware = new TurningMockHardware(motorD, sensor4);
        } else {
            this.motorA = new MediumMotorEV3(MotorPort.A);  //ConveyorMotor
            this.motorD = new LargeMotorEV3(MotorPort.D);   //TurningMotor

            this.sensor2 = new TouchSensorEV3(SensorPort.S2);    //LoadingSensor conveyor
            this.sensor3 = new ColorSensorEV3(SensorPort.S3);    //UnloadingSensor conveyor
            this.sensor4 = new TouchSensorEV3(SensorPort.S4);    //HomingSensor turning

            this.conveyorHardware = new LegoConveyorHardware(motorA, sensor2, sensor3);
            this.turningHardware = new LegoTurningHardware(motorD, sensor4);
        }
    }

    public Optional<Motor> getMotorA() {
        return Optional.of(motorA);
    }

    public Optional<Motor> getMotorB() {
        return Optional.of(motorB);
    }

    public Optional<Motor> getMotorC() {
        return Optional.of(motorC);
    }

    public Optional<Motor> getMotorD() {
        return Optional.of(motorD);
    }

    public Optional<Sensor> getSensor1() {
        return Optional.of(sensor1);
    }

    public Optional<Sensor> getSensor2() {
        return Optional.of(sensor2);
    }

    public Optional<Sensor> getSensor3() {
        return Optional.of(sensor3);
    }

    public Optional<Sensor> getSensor4() {
        return Optional.of(sensor4);
    }

    public Optional<ConveyorHardware> getConveyorHardware() {
        return Optional.of(conveyorHardware);
    }

    public Optional<TurningHardware> getTurningHardware() {
        return Optional.of(turningHardware);
    }
}
