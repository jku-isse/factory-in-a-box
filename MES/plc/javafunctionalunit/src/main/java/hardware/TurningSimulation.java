package hardware;

import hardware.actuators.Motor;
import hardware.sensors.Sensor;
import robot.turnTable.TurnTableOrientation;

import java.util.concurrent.atomic.AtomicBoolean;

import static robot.turnTable.TurnTableOrientation.NORTH;
import static robot.turnTable.TurnTableOrientation.WEST;

/**
 * This class should be used to simulate the behaviour of the turning funit.
 * The simulation has motors and sensors that interact with each other.
 * If you want to assign this to a funit, use the motors exposed though the getters.
 * Needs support for supporting error scenarios for testing.
 */
//TODO reimplement this entire simulation with better solution
public class TurningSimulation {

    private Motor turnMotor;
    private Sensor sensorHoming;

    private TurnTableOrientation orientation;
    private AtomicBoolean reachedHome;

    public TurningSimulation(int timerMs) {
        this.reachedHome = new AtomicBoolean(false);
        this.orientation = NORTH;
        this.turnMotor = new Motor() {
            @Override
            public void forward() {
                if (orientation == NORTH) {
                    System.out.println("Cannot turn from north to west");
                }
                while (!sensorHoming.hasDetectedInput()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void backward() {
                if (orientation == WEST) {
                    System.out.println("Cannot turn from west to north");
                }
                while (!sensorHoming.hasDetectedInput()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void stop() {

            }

            @Override
            public void setSpeed(int speed) {
                //Currently should not matter
            }

            @Override
            public void waitMs(long msDelay) {
                try {
                    Thread.sleep(msDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        this.sensorHoming = new Sensor() {
            @Override
            public boolean hasDetectedInput() {
                new Thread(() -> {
                    try {
                        Thread.sleep(timerMs);
                        reachedHome.set(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                return reachedHome.get();
            }
        };
    }

    /**
     * Returns the turning motor
     *
     * @return turnMotor
     */
    public Motor getTurnMotor() {
        return turnMotor;
    }

    /**
     * Returns the sensor used for homing
     *
     * @return sensorHoming
     */
    public Sensor getSensorHoming() {
        return sensorHoming;
    }
}
