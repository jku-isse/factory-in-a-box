package fiab.core.capabilities.functionalunit;

/**
 * Interface that specifies methods all FUs must have.
 * Each FU can be reset -> do_resetting
 * Each FU can be stopped -> do_stopping
 */
public interface BasicFUBehaviour {

    void doResetting();

    void doStopping();
}
