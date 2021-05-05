package config;

import actuators.*;
import hardware.*;
import actuators.motorsEV3.LargeMotorEV3;
import actuators.motorsEV3.MediumMotorEV3;
import lego.LegoConveyorHardware;
import lego.LegoInputStationHardware;
import lego.LegoPlotterHardware;
import lego.LegoTurningHardware;
import mock.MockConveyorHardware;
import mock.MockInputStationHardware;
import mock.MockOutputStationHardware;
import mock.MockPlotterHardware;
import mock.MockTurningHardware;
import sensors.MockSensor;
import sensors.Sensor;
import sensors.sensorsEV3.ColorSensorEV3;
import sensors.sensorsEV3.TouchSensorEV3;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;

import java.util.Optional;

public class HardwareInfo {

    private static final boolean DEBUG = System.getProperty("os.name").toLowerCase().contains("win");

    protected Motor motorA;
    protected Motor motorB;
    protected Motor motorC;
    protected Motor motorD;

    protected Sensor sensor1;
    protected Sensor sensor2;
    protected Sensor sensor3;
    protected Sensor sensor4;

    private ConveyorHardware conveyorHardware;
    private TurningHardware turningHardware;
    private InputStationHardware inputStationHardware;
    private OutputStationHardware outputStationHardware;
    private PlotterHardware plotterHardware;

    public HardwareInfo(MachineType machineType) {
        switch (machineType) {
            case INPUTSTATION:
                initInputStationHardware();
                break;
            case OUTPUTSTATION:
                initOutputStationHardware();
                break;
            case TURNTABLE:
                initTurntableHardware();
                break;
            case PLOTTER:
                initPlotterHardware();
                break;
            default:
                throw new RuntimeException("Could not configure Hardware for machineType: " + machineType);
        }
    }

    private void initInputStationHardware() {
        if (DEBUG) {
            this.motorA = new InputStationMockMotor();
            this.sensor1 = new MockSensor();
            this.inputStationHardware = new MockInputStationHardware(motorA, sensor1);
        } else {
            this.motorA = new LargeMotorEV3(MotorPort.A);
            this.sensor1 = new ColorSensorEV3(SensorPort.S1);
            this.inputStationHardware = new LegoInputStationHardware(motorA, sensor1);
        }
    }

    private void initOutputStationHardware() {
        if (DEBUG) {
            this.sensor1 = new MockSensor();
            outputStationHardware = new MockOutputStationHardware(sensor1);
        } else {
            this.sensor1 = new ColorSensorEV3(SensorPort.S1);
            outputStationHardware = new MockOutputStationHardware(sensor1);
        }
    }

    private void initTurntableHardware() {
        if (DEBUG) {
            this.motorA = new ConveyorMockMotor(100, 200);  //Mock ConveyorMotor
            this.motorD = new TurningMockMotor(200);   //Mock TurningMotor

            this.sensor2 = new MockSensor();    //Mock LoadingSensor conveyor
            this.sensor3 = new MockSensor();    //Mock UnloadingSensor conveyor
            this.sensor4 = new MockSensor();    //Mock HomingSensor turning
           
            this.conveyorHardware = new MockConveyorHardware(motorA, sensor2, sensor3);
            this.turningHardware = new MockTurningHardware(motorD, sensor4);
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

    private void initPlotterHardware() {
        if(DEBUG) {
        	this.motorA = new ConveyorMockMotor(100, 2000);
        	this.motorB = new PlotXMockMotor();   //MotorX (forward/backward)
        	this.motorC = new PlotYMockMotor();	//MotorY (left/right)
        	this.motorD = new MockMotor(100);	//PenMotor
        	
        	this.sensor1 = new MockSensor();
        	this.sensor2 = new MockSensor();
        	this.sensor3 = new MockSensor();
        	this.sensor4 = new MockSensor();
        	
        	this.conveyorHardware = new MockConveyorHardware(motorA, sensor2, sensor1);
        	this.plotterHardware = new MockPlotterHardware(motorB, motorC, motorD, sensor3, sensor4);
        }else {
        	this.motorA = new LargeMotorEV3(MotorPort.A);
        	this.motorB = new LargeMotorEV3(MotorPort.B);
        	this.motorC = new LargeMotorEV3(MotorPort.C);
        	this.motorD = new MediumMotorEV3(MotorPort.D);
        	
        	this.sensor1 = new ColorSensorEV3(SensorPort.S1);
        	this.sensor2 = new TouchSensorEV3(SensorPort.S2);
        	this.sensor3 = new TouchSensorEV3(SensorPort.S3);
        	this.sensor4 = new TouchSensorEV3(SensorPort.S4);
        	
        	this.conveyorHardware = new LegoConveyorHardware(motorA, sensor2, sensor3);
        	this.plotterHardware = new LegoPlotterHardware(motorB, motorC, motorD, sensor3, sensor4);
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

    public Optional<PlotterHardware> getPlotterHardware() {
        return Optional.of(plotterHardware);
    }

    public Optional<InputStationHardware> getInputStationHardware() {
        return Optional.of(inputStationHardware);
    }

    public Optional<OutputStationHardware> getOutputStationHardware() {
        return Optional.of(outputStationHardware);
    }
}
