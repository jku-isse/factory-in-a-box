package fiab.handshake.server.statemachine;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.observer.FUStateObserver;
import fiab.handshake.server.TransportAreaStatusInfo;

public class ServerSideStateMachine extends StateMachine<ServerSideStates, Object> implements FUStateObserver {

    /**
     * Creates a StateMachine for the Server Side handshake.
     * By changing the TransportAreaStatusInfo, the handshake will reset in an empty or loaded state
     *
     * @param info info about the loaded/unloaded state for idle and ready transitions
     */
    public ServerSideStateMachine(TransportAreaStatusInfo info) {
        this(info, ServerSideStates.STOPPED);
    }

    /**
     * Use this constructor for testing.
     * Note that starting in a different state might cause the handshake to fail in production
     *
     * @param initialState state in which the handshake will start
     */
    public ServerSideStateMachine(TransportAreaStatusInfo info, ServerSideStates initialState) {
        super(initialState, new ServerSideStateMachineConfig(info));
    }

    public void fireIfPossible(Object trigger) {
        if (canFire(trigger)) {
            fire(trigger);
        }
    }

    @Override
    public void notifyAboutStateChange(Object currentState) {
        fireIfPossible(currentState);
    }

    static class ServerSideStateMachineConfig extends StateMachineConfig<ServerSideStates, Object> {

        private final TransportAreaStatusInfo info;
        private ServerSideStates idleState;

        public ServerSideStateMachineConfig(TransportAreaStatusInfo info) {
            this.info = info;
            configure(ServerSideStates.RESETTING)
                    .permitIf(ServerSideHandshakeTriggers.RESETTING_DONE, ServerSideStates.IDLE_EMPTY,
                            () -> isTransportAreaEmpty())
                    .permitIf(ServerSideHandshakeTriggers.RESETTING_DONE, ServerSideStates.IDLE_LOADED,
                            () -> isTransportAreaLoaded())
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.IDLE_EMPTY)
                    .onEntry(() -> idleState = ServerSideStates.IDLE_EMPTY) //Remember idle state for ready
                    .permit(ServerSideHandshakeTriggers.START, ServerSideStates.STARTING)
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.IDLE_LOADED)
                    .onEntry(() -> idleState = ServerSideStates.IDLE_LOADED) //Remember idle state for ready
                    .permit(ServerSideHandshakeTriggers.START, ServerSideStates.STARTING)
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.STARTING)
                    .permit(ServerSideHandshakeTriggers.PREPARE, ServerSideStates.PREPARING)
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.PREPARING)
                    .permitIf(ServerSideHandshakeTriggers.READY, ServerSideStates.READY_EMPTY,
                            () -> isTransportAreaEmpty() && checkTransportAreaWasInIdleEmpty())
                    .permitIf(ServerSideHandshakeTriggers.READY, ServerSideStates.READY_LOADED,
                            () -> isTransportAreaLoaded() && checkTransportAreaWasInIdleLoaded())
                    .permitIf(ServerSideHandshakeTriggers.READY, ServerSideStates.STOPPING, () ->
                            checkTransportAreaMismatch())
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.READY_EMPTY)
                    .permitIf(ServerSideHandshakeTriggers.EXECUTE, ServerSideStates.STOPPING, () -> checkTransportAreaMismatch())
                    .permitIf(ServerSideHandshakeTriggers.EXECUTE, ServerSideStates.EXECUTE, () -> !checkTransportAreaMismatch())
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.READY_LOADED)
                    .permitIf(ServerSideHandshakeTriggers.EXECUTE, ServerSideStates.STOPPING, () -> checkTransportAreaMismatch())
                    .permitIf(ServerSideHandshakeTriggers.EXECUTE, ServerSideStates.EXECUTE, () -> !checkTransportAreaMismatch())
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.EXECUTE)
                    .permit(ServerSideHandshakeTriggers.COMPLETE, ServerSideStates.COMPLETING)
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.COMPLETING)
                    .permit(ServerSideHandshakeTriggers.COMPLETING_DONE, ServerSideStates.COMPLETE)
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.COMPLETE)
                    .permit(ServerSideHandshakeTriggers.RESET, ServerSideStates.RESETTING)
                    .permit(ServerSideHandshakeTriggers.STOP, ServerSideStates.STOPPING);

            configure(ServerSideStates.STOPPING)
                    .permit(ServerSideHandshakeTriggers.STOPPING_DONE, ServerSideStates.STOPPED);

            configure(ServerSideStates.STOPPED)
                    .permit(ServerSideHandshakeTriggers.RESET, ServerSideStates.RESETTING);
        }

        private boolean isTransportAreaEmpty() {
            return info.isTransportAreaEmpty();
        }

        private boolean isTransportAreaLoaded() {
            return !info.isTransportAreaEmpty();
        }

        private boolean checkTransportAreaWasInIdleEmpty() {
            return idleState == ServerSideStates.IDLE_EMPTY;
        }

        private boolean checkTransportAreaWasInIdleLoaded() {
            return idleState == ServerSideStates.IDLE_LOADED;
        }

        private boolean checkTransportAreaMismatch(){
            return !(isTransportAreaEmpty() && checkTransportAreaWasInIdleEmpty())
                    && !(isTransportAreaLoaded() && checkTransportAreaWasInIdleLoaded());
        }
    }
}
