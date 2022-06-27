package fiab.functionalunit.observer;

public interface FUStateChangedSubject {

    void addSubscriber(FUStateObserver observer);

    void removeSubscriber(FUStateObserver observer);

    void notifySubscribers(Object state);
}
