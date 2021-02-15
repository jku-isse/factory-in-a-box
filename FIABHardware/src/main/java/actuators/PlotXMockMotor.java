package actuators;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import sensors.MockSensor;

public class PlotXMockMotor extends MockMotor {

	private MockSensor sensorHoming;
	private ScheduledFuture homingTask;
	private long delay;

	public PlotXMockMotor() {
		super(200);
		this.delay = 2000;
	}

	public void setSensorHoming(MockSensor sensorHoming) {
		this.sensorHoming = sensorHoming;
		this.sensorHoming.setDetectedInput(true); // When initialized, we assume machine is in stopped state
	}

	@Override
	public void forward() {
		super.forward();
		sensorHoming.setDetectedInput(false);
	}

	@Override
	public void backward() {
		super.backward();
		homingTask = executorService.schedule(() -> sensorHoming.setDetectedInput(true), delay, TimeUnit.MILLISECONDS);
	}

	@Override
	public void stop() {
		super.stop();
		if (homingTask != null) {
			homingTask.cancel(true);
		}
	}

}
