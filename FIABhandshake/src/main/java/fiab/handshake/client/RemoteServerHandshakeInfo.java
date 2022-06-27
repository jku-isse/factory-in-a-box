package fiab.handshake.client;

import akka.actor.ActorRef;
import fiab.functionalunit.observer.FUStateChangedSubject;
import fiab.functionalunit.observer.FUStateObserver;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;

import java.util.HashSet;
import java.util.Set;

public class RemoteServerHandshakeInfo implements FUStateChangedSubject {

    private Set<FUStateObserver> observers;
    private ServerSideStates remoteState;
    private ServerSideStates loadedStatusDuringInit;
    private ServerSideStates loadedStatusDuringStart;
    private ActorRef serverSide;
    private HandshakeCapability.ServerMessageTypes serverResponse;

    public RemoteServerHandshakeInfo(){
        observers = new HashSet<>();
        remoteState = ServerSideStates.UNKNOWN;
        loadedStatusDuringInit = null;
        loadedStatusDuringStart = null;
        serverResponse = null;
    }

    public ServerSideStates getRemoteState() {
        return remoteState;
    }

    public void setRemoteState(ServerSideStates remoteState) {
        if(remoteState == ServerSideStates.IDLE_EMPTY || remoteState == ServerSideStates.IDLE_LOADED){
            setLoadedStatusDuringInit(remoteState);
        }
        if(remoteState == ServerSideStates.READY_EMPTY || remoteState == ServerSideStates.READY_LOADED){
            setLoadedStatusDuringStart(remoteState);
        }
        this.remoteState = remoteState;
        notifySubscribers(this.remoteState);
    }

    public ActorRef getServerSide() {
        return serverSide;
    }

    public void setServerSide(ActorRef serverSide) {
        this.serverSide = serverSide;
    }

    public HandshakeCapability.ServerMessageTypes getServerResponse() {
        return serverResponse;
    }

    public void setServerResponse(HandshakeCapability.ServerMessageTypes serverResponse) {
        this.serverResponse = serverResponse;
        notifySubscribers(this.serverResponse);
    }

    public ServerSideStates getLoadedStatusDuringInit() {
        return loadedStatusDuringInit;
    }

    private void setLoadedStatusDuringInit(ServerSideStates loadedStatusDuringInit) {
        this.loadedStatusDuringInit = loadedStatusDuringInit;
    }

    public ServerSideStates getLoadedStatusDuringStart() {
        return loadedStatusDuringStart;
    }

    private void setLoadedStatusDuringStart(ServerSideStates loadedStatusDuringStart){
        this.loadedStatusDuringStart = loadedStatusDuringStart;
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
        for(FUStateObserver observer : observers){
            observer.notifyAboutStateChange(state);
        }
    }
}
