package lego;

import hardware.PlotterHardware;
import actuators.Motor;
import sensors.Sensor;

public class LegoPlotterHardware extends PlotterHardware{

	//Plotters are currently running on 4diac 
	public LegoPlotterHardware(Motor motorX, Motor motorY, Motor penMotor, Sensor sensorX, Sensor sensorY) {
		this.motorX = motorX;
		this.motorY = motorY;
		this.penMotor = penMotor;
		this.sensorX = sensorX;
		this.sensorY = sensorY;
	}
}
