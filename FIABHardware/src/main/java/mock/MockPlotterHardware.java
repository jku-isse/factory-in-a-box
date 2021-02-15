package mock;

import hardware.PlotterHardware;
import actuators.Motor;
import sensors.Sensor;

public class MockPlotterHardware extends PlotterHardware {
	//Currently does not simulate any behaviour
	public MockPlotterHardware(Motor motorX, Motor motorY, Motor penMotor, Sensor sensorX, Sensor sensorY) {
		this.motorX = motorX;
		this.motorY = motorY;
		this.penMotor = penMotor;
		this.sensorX = sensorX;
		this.sensorY = sensorY;
	}

}
