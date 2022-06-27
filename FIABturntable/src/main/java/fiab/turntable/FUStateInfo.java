package fiab.turntable;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.observer.FUStateChangedSubject;
import fiab.functionalunit.observer.FUStateObserver;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.turntable.turning.statemachine.TurningStates;

import java.util.HashSet;
import java.util.Set;

public class FUStateInfo implements FUStateChangedSubject {

    private final Set<FUStateObserver> observers;

    private boolean isTransportAreaEmpty;
    private TurningStates turningFuState;
    private ConveyorStates conveyorFuState;
    private HandshakeEndpointInfo handshakeEndpointInfo;

    public FUStateInfo(ActorRef actorRef) {
        this(actorRef, TurningStates.STOPPED, ConveyorStates.STOPPED);
    }

    public FUStateInfo(ActorRef actorRef, TurningStates turningFuState, ConveyorStates conveyorFuState) {
        observers = new HashSet<>();
        isTransportAreaEmpty = true;    //We assume it is empty, but it will change once conveyor is idle
        this.turningFuState = turningFuState;
        this.conveyorFuState = conveyorFuState;
        this.handshakeEndpointInfo = new HandshakeEndpointInfo(actorRef);
    }

    public boolean isTransportAreaEmpty() {
        return isTransportAreaEmpty;
    }

    public TurningStates getTurningFuState() {
        return turningFuState;
    }

    public void setTurningFuState(TurningStates turningFuState) {
        this.turningFuState = turningFuState;
        notifySubscribers(this.turningFuState);
    }

    public ConveyorStates getConveyorFuState() {
        return conveyorFuState;
    }

    public void setConveyorFuState(ConveyorStates conveyorFuState) {
        if (conveyorFuState == ConveyorStates.IDLE_EMPTY) isTransportAreaEmpty = true;
        if (conveyorFuState == ConveyorStates.IDLE_FULL) isTransportAreaEmpty = false;
        this.conveyorFuState = conveyorFuState;
        notifySubscribers(this.conveyorFuState);
    }

    public void updateServerHandshakeState(String capabilityId, ServerSideStates state){
        handshakeEndpointInfo.updateServerHandshakeState(capabilityId, state);
        notifySubscribers(state);
    }

    public void updateClientHandshakeState(String capabilityId, ClientSideStates state){
        handshakeEndpointInfo.updateClientHandshakeState(capabilityId, state);
        notifySubscribers(state);
    }

    public HandshakeEndpointInfo getHandshakeEndpointInfo() {
        return handshakeEndpointInfo;
    }

    public void setHandshakeEndpointInfo(HandshakeEndpointInfo handshakeEndpointInfo) {
        this.handshakeEndpointInfo = handshakeEndpointInfo;
    }

    @Override
    public void addSubscriber(FUStateObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void removeSubscriber(FUStateObserver observer) {
        this.observers.remove(observer);
    }

    @Override
    public void notifySubscribers(Object state) {
        for (FUStateObserver observer : observers){
            observer.notifyAboutStateChange(state);
        }
    }
}
