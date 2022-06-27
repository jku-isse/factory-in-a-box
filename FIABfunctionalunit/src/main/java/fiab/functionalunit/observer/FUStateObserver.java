package fiab.functionalunit.observer;

public interface FUStateObserver {

    //We need to pass the current state as a parameter, since there might be different states we listen to
    //The type is currently Object, since we don't know which (enum) type the State will be
    void notifyAboutStateChange(Object currentState);
}
