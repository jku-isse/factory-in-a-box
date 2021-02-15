package hardware;

import actuators.Motor;
import sensors.Sensor;

public abstract class PlotterHardware {
	
	protected Motor motorX;
	protected Motor motorY;
	protected Motor penMotor;
	protected Sensor sensorX;
	protected Sensor sensorY;
	
	public Motor getMotorX() {
		return motorX;
	}
	
	public void setMotorX(Motor motorX) {
		this.motorX = motorX;
	}
	
	public Motor getMotorY() {
		return motorY;
	}
	
	public void setMotorY(Motor motorY) {
		this.motorY = motorY;
	}
	
	public Motor getPenMotor() {
		return penMotor;
	}
	
	public void setPenMotor(Motor penMotor) {
		this.penMotor = penMotor;
	}
	
	public Sensor getSensorX() {
		return sensorX;
	}
	
	public void setSensorX(Sensor sensorX) {
		this.sensorX = sensorX;
	}
	
	public Sensor getSensorY() {
		return sensorY;
	}
	
	public void setSensorY(Sensor sensorY) {
		this.sensorY = sensorY;
	}
	
}
