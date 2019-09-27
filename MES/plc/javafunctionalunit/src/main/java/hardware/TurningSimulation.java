package hardware;

import hardware.actuators.Motor;
import hardware.sensors.Sensor;
import robot.turnTable.TurnTableOrientation;

import java.util.concurrent.atomic.AtomicBoolean;

import static robot.turnTable.TurnTableOrientation.NORTH;
import static robot.turnTable.TurnTableOrientation.WEST;

public class TurningSimulation {

    private Motor turnMotor;
    private Sensor sensorHoming;

    private TurnTableOrientation orientation;
    private AtomicBoolean reachedHome;
    private int timerMs;

    public TurningSimulation(int timerMs) {
        this.reachedHome = new AtomicBoolean(false);
        this.orientation = NORTH;
        this.turnMotor = new Motor() {
            @Override
            public void forward() {
                if(orientation == NORTH){
                    System.out.println("Cannot turn from north to west");
                }
                while(!sensorHoming.detectedInput()){
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void backward() {
                if(orientation == WEST){
                    System.out.println("Cannot turn from west to north");
                }while(!sensorHoming.detectedInput()){
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
            public void waitMs(int msDelay) {
                try {
                    Thread.sleep(msDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        this.sensorHoming = new Sensor() {
            @Override
            public boolean detectedInput() {
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

    public Motor getTurnMotor() {
        return turnMotor;
    }

    public Sensor getSensorHoming() {
        return sensorHoming;
    }
}
