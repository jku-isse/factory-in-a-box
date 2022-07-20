package fiab.plotter;

import akka.actor.ActorRef;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.observer.FUStateChangedSubject;
import fiab.functionalunit.observer.FUStateObserver;

import java.util.HashSet;
import java.util.Set;

public class FUStateInfo implements FUStateChangedSubject {

    private final Set<FUStateObserver> observers;
    private BasicMachineStates plottingState;
    private ConveyorStates conveyorFUState;
    private ServerSideStates handshakeFUState;

    public FUStateInfo(ActorRef actorRef) {
        this(actorRef, BasicMachineStates.STOPPED, ConveyorStates.STOPPED, ServerSideStates.STOPPED);
    }

    public FUStateInfo(ActorRef actorRef, BasicMachineStates plottingStates, ConveyorStates conveyorFUState, ServerSideStates handshakeFUState) {
        observers = new HashSet<>();
        this.plottingState = plottingStates;
        this.conveyorFUState = conveyorFUState;
        this.handshakeFUState = handshakeFUState;
    }

    public void setPlottingState(BasicMachineStates plottingStates) {
        this.plottingState = plottingStates;
        notifySubscribers(plottingState);
    }

    public void setConveyorFUState(ConveyorStates conveyorState) {
        this.conveyorFUState = conveyorState;
        notifySubscribers(conveyorState);
    }

    public void setHandshakeFUState(ServerSideStates handshakeState) {
        this.handshakeFUState = handshakeState;
        notifySubscribers(handshakeState);
    }

    public BasicMachineStates getPlottingState() {
        return plottingState;
    }

    public ConveyorStates getConveyorFUState() {
        return conveyorFUState;
    }

    public ServerSideStates getHandshakeFUState() {
        return handshakeFUState;
    }

    @Override
    public void addSubscriber(FUStateObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeSubscriber(FUStateObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifySubscribers(Object state) {
        for (FUStateObserver observer : observers) {
            observer.notifyAboutStateChange(state);
        }
    }
}
