package functional_unit_base;

public abstract class ProcessEngineBase {

    public abstract void loadProcess();

    public abstract void reset();   //TODO delete this if not necessary

    public abstract void stop();
}
