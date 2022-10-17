package fiab.mes.machine.actor.roboticArm.wrapper;

public interface RoboticArmWrapperInterface {

    public void pick(String partId);

    public void stop();

    public void reset();

    void subscribeToStatus();
}
