package fiab.turntable.statemachine.turntable;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.observer.FUStateObserver;
import fiab.turntable.FUStateInfo;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.turntable.turning.statemachine.TurningStates;

import static fiab.turntable.statemachine.turntable.TurntableTriggers.*;

public class TurntableStateMachine extends StateMachine<BasicMachineStates, Object> implements FUStateObserver {

    public TurntableStateMachine(FUStateInfo info) {
        super(BasicMachineStates.STOPPED, new TurntableStateMachineConfig(info));
        info.addSubscriber(this);
    }

    @Override
    public void notifyAboutStateChange(Object currentState) {
        if (canFire(currentState)) {
            fire(currentState);
        }
    }

    static class TurntableStateMachineConfig extends StateMachineConfig<BasicMachineStates, Object> {

        private final FUStateInfo fuStateInfo;

        TurntableStateMachineConfig(FUStateInfo fuStateInfo) {
            this.fuStateInfo = fuStateInfo;
            configure(BasicMachineStates.IDLE)
                    .permit(START, BasicMachineStates.STARTING)
                    .permit(STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.STARTING)
                    .permit(EXECUTE, BasicMachineStates.EXECUTE)
                    .permit(STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.EXECUTE)
                    .permit(COMPLETE, BasicMachineStates.COMPLETING)
                    .permit(STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.COMPLETING)
                    .permit(COMPLETING_DONE, BasicMachineStates.COMPLETE)
                    .permit(STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.COMPLETE)
                    .permit(RESET, BasicMachineStates.RESETTING)
                    .permit(STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.STOPPING)
                    .permitIf(TurningStates.STOPPED, BasicMachineStates.STOPPED, () -> checkForStoppedTransition())
                    .permitIf(ConveyorStates.STOPPED, BasicMachineStates.STOPPED, () -> checkForStoppedTransition())
                    .permitIf(ServerSideStates.STOPPED, BasicMachineStates.STOPPED, () -> checkForStoppedTransition())
                    .permitIf(ClientSideStates.STOPPED, BasicMachineStates.STOPPED, () -> checkForStoppedTransition())
                    .permit(STOP_DONE, BasicMachineStates.STOPPED);

            configure(BasicMachineStates.STOPPED)
                    .permit(RESET, BasicMachineStates.RESETTING);

            configure(BasicMachineStates.RESETTING)
                    .permitIf(TurningStates.IDLE, BasicMachineStates.IDLE, () -> checkForIdleTransition())
                    .permitIf(ConveyorStates.IDLE_EMPTY, BasicMachineStates.IDLE, () -> checkForIdleTransition())
                    .permitIf(ConveyorStates.IDLE_FULL, BasicMachineStates.IDLE, () -> checkForIdleTransition())
                    .permit(RESET_DONE, BasicMachineStates.IDLE)
                    .permit(STOP, BasicMachineStates.STOPPING);
        }

        protected boolean checkForIdleTransition() {
            return fuStateInfo.getTurningFuState() == TurningStates.IDLE
                    && (fuStateInfo.getConveyorFuState() == ConveyorStates.IDLE_EMPTY ||
                    fuStateInfo.getConveyorFuState() == ConveyorStates.IDLE_FULL);
        }

        protected boolean checkForStoppedTransition() {
            return fuStateInfo.getTurningFuState() == TurningStates.STOPPED
                    && fuStateInfo.getConveyorFuState() == ConveyorStates.STOPPED
                    && fuStateInfo.getHandshakeEndpointInfo().allHandshakesStopped();
        }

    }
}
