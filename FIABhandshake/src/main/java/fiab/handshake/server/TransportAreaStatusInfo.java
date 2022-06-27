package fiab.handshake.server;

import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.functionalunit.observer.FUStateChangedSubject;
import fiab.functionalunit.observer.FUStateObserver;

import java.util.HashSet;
import java.util.Set;

/**
 * This class tracks the state of the Transport Area on the machine.
 * It could represent either a conveyor belt, an area where objects could be picked up, etc...
 * Tracking this we can ensure the handshake always resets in the correct state
 */
public class TransportAreaStatusInfo implements FUStateChangedSubject {

    private final Set<FUStateObserver> subscribers;
    private boolean transportAreaIsEmpty;

    public TransportAreaStatusInfo(){
        this.subscribers = new HashSet<>();
        //We assume the area is free when starting the machine
        //Before resetting the handshake, we should receive the correct status from the coordinator anyway
        this.transportAreaIsEmpty = true;
    }

    public void updateTransportAreaStatus(HandshakeCapability.StateOverrideRequests stateOverrideRequest) {
        this.transportAreaIsEmpty = stateOverrideRequest == HandshakeCapability.StateOverrideRequests.SetEmpty;
        notifySubscribers(this.transportAreaIsEmpty);
    }

    public boolean isTransportAreaEmpty() {
        return transportAreaIsEmpty;
    }

    @Override
    public void addSubscriber(FUStateObserver observer) {
        subscribers.add(observer);
    }

    @Override
    public void removeSubscriber(FUStateObserver observer) {
        subscribers.remove(observer);
    }

    @Override
    public void notifySubscribers(Object state) {
        for(FUStateObserver subscriber : subscribers){
            subscriber.notifyAboutStateChange(state);
        }
    }
}
