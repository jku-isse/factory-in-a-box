package hardware.actuators;

import java.util.concurrent.atomic.AtomicBoolean;

public class MockMotor extends Motor {

    private AtomicBoolean running;

    public MockMotor() {
        running = new AtomicBoolean(false);
    }

    @Override
    public void forward() {
        new Thread(() -> {
            running.set(true);
            while (isRunning()) {
                try {
                    System.out.println("Moving forward ...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void backward() {
        new Thread(() -> {
            running.set(true);
            while (isRunning()) {
                try {
                    System.out.println("Moving backward ...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void stop() {
        System.out.println("Motor stopped");
        running.set(false);
    }

    @Override
    public void setSpeed(int speed) {
        System.out.println("Set speed to " + speed);
    }

    @Override
    public void waitMs(int msDelay) {
        new Thread(() -> {
            try {
                Thread.sleep(msDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }


    private boolean isRunning() {
        return running.get();
    }
}
