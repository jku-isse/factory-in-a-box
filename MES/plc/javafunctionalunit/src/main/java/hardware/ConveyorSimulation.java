package hardware;

import hardware.actuators.Motor;
import hardware.sensors.Sensor;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConveyorSimulation {

    private Motor conveyorMotor;
    private Sensor sensorLoading;
    private Sensor sensorUnloading;

    private Timer timer;

    private int timerMs;

    private AtomicBoolean fullyLoaded;
    private AtomicBoolean fullyUnloaded;
    private AtomicBoolean stopped;

    public ConveyorSimulation(int timerMs) {
        this.timer = new Timer();
        this.fullyLoaded = new AtomicBoolean(false);
        this.fullyUnloaded = new AtomicBoolean(true);
        this.stopped = new AtomicBoolean(false);
        this.timerMs = timerMs;
        this.conveyorMotor = new Motor() {
            @Override
            public void forward() {
                if (fullyLoaded.get()) {
                    System.out.println("Conveyor is already loaded");
                } else {
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            fullyLoaded.set(true);
                        }
                    }, timerMs);
                    while (!sensorLoading.detectedInput()) {
                        if (stopped.get()) {
                            stopped.set(false);
                            return;
                        }
                    }
                }
            }

            @Override
            public void backward() {
                if (fullyUnloaded.get()) {
                    System.out.println("Conveyor is empty");
                } else {
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            fullyUnloaded.set(true);
                        }
                    }, timerMs);
                    while (!sensorUnloading.detectedInput()) {
                        if (stopped.get()) {
                            stopped.set(false);
                            return;
                        }
                    }
                }
            }

            @Override
            public void stop() {
                stopped.set(true);
                fullyLoaded.set(false);
                fullyUnloaded.set(true);
            }

            @Override
            public void setSpeed(int speed) {
                //does not matter here for now
            }

            @Override
            public void waitMs(int msDelay) {
                try {
                    Thread.sleep(msDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        this.sensorLoading = new Sensor() {
            @Override
            public boolean detectedInput() {
                return fullyLoaded.get();
            }
        };

        this.sensorUnloading = new Sensor() {
            @Override
            public boolean detectedInput() {
                return fullyUnloaded.get();
            }
        };

    }

    public Motor getConveyorMotor() {
        return conveyorMotor;
    }

    public Sensor getSensorLoading() {
        return sensorLoading;
    }

    public Sensor getSensorUnloading() {
        return sensorUnloading;
    }
}
