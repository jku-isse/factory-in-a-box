package fiab.handshake.client.statemachine;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import fiab.functionalunit.observer.FUStateObserver;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.handshake.client.RemoteServerHandshakeInfo;

public class ClientSideStateMachine extends StateMachine<ClientSideStates, Object> implements FUStateObserver {

    private final RemoteServerHandshakeInfo remoteServerHandshakeInfo;

    /**
     * This constructor creates a ClientSideStateMachine
     * The remoteInfo is necessary for defining Guards inside the state machine and to trigger events
     *
     * @param remoteInfo
     */
    public ClientSideStateMachine(RemoteServerHandshakeInfo remoteInfo) {
        super(ClientSideStates.STOPPED, new ClientSideStateMachineConfig(remoteInfo));
        this.remoteServerHandshakeInfo = remoteInfo;
        this.remoteServerHandshakeInfo.addSubscriber(this);
    }

    /**
     * This constructor makes testing easier, since we can set the initial state
     * Please note that the remote info has not been initialized yet
     *
     * @param initialState
     * @param remoteInfo
     */
    public ClientSideStateMachine(ClientSideStates initialState, RemoteServerHandshakeInfo remoteInfo) {
        super(initialState, new ClientSideStateMachineConfig(remoteInfo));
        this.remoteServerHandshakeInfo = remoteInfo;
        this.remoteServerHandshakeInfo.addSubscriber(this);
    }

    public void fireIfPossible(Object trigger){
        if(canFire(trigger)){
            fire(trigger);
        }
    }

    @Override
    public void notifyAboutStateChange(Object currentState) {
        fireIfPossible(currentState);
    }

    public static class ClientSideStateMachineConfig extends StateMachineConfig<ClientSideStates, Object> {

        private final RemoteServerHandshakeInfo remoteInfo;

        public ClientSideStateMachineConfig(RemoteServerHandshakeInfo info) {
            remoteInfo = info;
            configure(ClientSideStates.STOPPING)
                    .permit(ClientSideHandshakeTriggers.STOPPING_DONE, ClientSideStates.STOPPED);

            configure(ClientSideStates.STOPPED)
                    .permit(ClientSideHandshakeTriggers.RESET, ClientSideStates.RESETTING);

            configure(ClientSideStates.RESETTING)
                    .permit(ClientSideHandshakeTriggers.RESETTING_DONE, ClientSideStates.IDLE)
                    .permit(ClientSideHandshakeTriggers.STOP, ClientSideStates.STOPPING);

            configure(ClientSideStates.IDLE)
                    .permit(ClientSideHandshakeTriggers.START, ClientSideStates.STARTING)
                    .permit(ClientSideHandshakeTriggers.STOP, ClientSideStates.STOPPING);

            configure(ClientSideStates.STARTING)
                    .permit(ServerSideStates.IDLE_EMPTY, ClientSideStates.INITIATING)
                    .permit(ServerSideStates.IDLE_LOADED, ClientSideStates.INITIATING)
                    .permit(ClientSideHandshakeTriggers.STOP, ClientSideStates.STOPPING);

            configure(ClientSideStates.INITIATING)
                    .permitIf(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover, ClientSideStates.INITIATED,
                            () -> checkInitiatingToInitiatedTransition())
                    .permitIf(ServerSideStates.STARTING, ClientSideStates.INITIATED, () -> checkInitiatingToInitiatedTransition())
                    .permit(HandshakeCapability.ServerMessageTypes.NotOkResponseInitHandover, ClientSideStates.STOPPING)
                    .permit(ClientSideHandshakeTriggers.STOP, ClientSideStates.STOPPING);

            configure(ClientSideStates.INITIATED)
                    .permit(ClientSideHandshakeTriggers.READY, ClientSideStates.READY)
                    .permit(ServerSideStates.READY_EMPTY, ClientSideStates.READY)
                    .permit(ServerSideStates.READY_LOADED, ClientSideStates.READY)
                    .permit(ClientSideHandshakeTriggers.STOP, ClientSideStates.STOPPING);

            configure(ClientSideStates.READY)
                    .permitIf(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover, ClientSideStates.EXECUTE,
                            () -> checkReadyToExecuteTransition())
                    .permitIf(ServerSideStates.READY_EMPTY, ClientSideStates.EXECUTE,
                            () -> checkReadyToExecuteTransition())
                    .permitIf(ServerSideStates.READY_LOADED, ClientSideStates.EXECUTE,
                            () -> checkReadyToExecuteTransition())
                    .permit(ClientSideHandshakeTriggers.STOP, ClientSideStates.STOPPING)
                    .permit(HandshakeCapability.ServerMessageTypes.NotOkResponseStartHandover, ClientSideStates.STOPPING)
                    .permitIf(ServerSideStates.READY_EMPTY, ClientSideStates.STOPPING,
                            () -> checkReadyInWrongLoadedState())
                    .permitIf(ServerSideStates.READY_LOADED, ClientSideStates.STOPPING,
                            () -> checkReadyInWrongLoadedState())
                    .permit(ServerSideStates.STOPPING, ClientSideStates.STOPPING)
                    .permit(ServerSideStates.STOPPED, ClientSideStates.STOPPING);

            configure(ClientSideStates.EXECUTE)
                    //add guard to transition so we only complete the handshake once both machines are ready?
                    .permit(ClientSideHandshakeTriggers.COMPLETE, ClientSideStates.COMPLETING)
                    .permit(ClientSideHandshakeTriggers.STOP, ClientSideStates.STOPPING)
                    .permit(ServerSideStates.STOPPING, ClientSideStates.STOPPING)
                    .permit(ServerSideStates.STOPPED, ClientSideStates.STOPPING);

            configure(ClientSideStates.COMPLETING)
                    .permit(ClientSideHandshakeTriggers.COMPLETING_DONE, ClientSideStates.COMPLETED)
                    .permit(ClientSideHandshakeTriggers.STOP, ClientSideStates.STOPPING);

            configure(ClientSideStates.COMPLETED)
                    .permit(ClientSideHandshakeTriggers.RESET, ClientSideStates.RESETTING)
                    .permit(ClientSideHandshakeTriggers.STOP, ClientSideStates.STOPPING);
        }

        private boolean checkInitiatingToInitiatedTransition() {
            ServerSideStates remoteState = remoteInfo.getRemoteState();
            return remoteInfo.getServerResponse() == HandshakeCapability.ServerMessageTypes.OkResponseInitHandover
                    && (remoteState != ServerSideStates.IDLE_EMPTY || remoteState != ServerSideStates.IDLE_LOADED)
                    && (remoteState != ServerSideStates.STOPPING || remoteState != ServerSideStates.STOPPED);
        }

        private boolean checkReadyToExecuteTransition() {
            return remoteInfo.getServerResponse() == HandshakeCapability.ServerMessageTypes.OkResponseStartHandover
                    && (remoteInitAndReadyStateHaveMatchLoadingStatus());
        }

        private boolean checkReadyInWrongLoadedState() {
            return (isRemoteReadyEmpty() || isRemoteReadyLoaded()) &&
                    !remoteInitAndReadyStateHaveMatchLoadingStatus();
        }

        private boolean remoteInitAndReadyStateHaveMatchLoadingStatus() {
            return (remoteInitialisedInEmptyState() && remoteReadyInEmptyState()) ||
                    (remoteInitialisedInLoadedState() && remoteReadyInLoadedState());
        }

        private boolean remoteInitialisedInEmptyState() {
            return remoteInfo.getLoadedStatusDuringInit() == ServerSideStates.IDLE_EMPTY;
        }

        private boolean remoteInitialisedInLoadedState() {
            return remoteInfo.getLoadedStatusDuringInit() == ServerSideStates.IDLE_LOADED;
        }

        private boolean remoteReadyInEmptyState(){
            return remoteInfo.getLoadedStatusDuringStart() == ServerSideStates.READY_EMPTY;
        }

        private boolean remoteReadyInLoadedState(){
            return remoteInfo.getLoadedStatusDuringStart() == ServerSideStates.READY_LOADED;
        }

        private boolean isRemoteReadyEmpty() {
            return remoteInfo.getRemoteState() == ServerSideStates.READY_EMPTY;
        }

        private boolean isRemoteReadyLoaded() {
            return remoteInfo.getRemoteState() == ServerSideStates.READY_LOADED;
        }
    }
}
