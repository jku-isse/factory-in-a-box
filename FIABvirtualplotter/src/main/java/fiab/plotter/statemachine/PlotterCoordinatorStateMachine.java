package fiab.plotter.statemachine;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.plotter.FUStateInfo;

public class PlotterCoordinatorStateMachine extends StateMachine<BasicMachineStates, Object> {


    public PlotterCoordinatorStateMachine(FUStateInfo info) {
        super(BasicMachineStates.STOPPED, new PlotterStateMachineConfig(info));
    }

    static class PlotterStateMachineConfig extends StateMachineConfig<BasicMachineStates, Object> {

        private final FUStateInfo fuStateInfo;

        PlotterStateMachineConfig(FUStateInfo fuStateInfo) {
            this.fuStateInfo = fuStateInfo;
            configure(BasicMachineStates.IDLE)
                    .permit(PlotterTriggers.START, BasicMachineStates.STARTING)
                    .permit(PlotterTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.STARTING)
                    .permit(PlotterTriggers.EXECUTE, BasicMachineStates.EXECUTE)
                    .permit(PlotterTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.EXECUTE)
                    .permit(PlotterTriggers.COMPLETE, BasicMachineStates.COMPLETING)
                    .permit(PlotterTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.COMPLETING)
                    .permit(PlotterTriggers.COMPLETING_DONE, BasicMachineStates.COMPLETE)
                    .permit(PlotterTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.COMPLETE)
                    .permit(PlotterTriggers.RESET, BasicMachineStates.RESETTING)
                    .permit(PlotterTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.STOPPING)
                    .permitIf(ConveyorStates.STOPPED, BasicMachineStates.STOPPED, () -> checkForStoppedTransition())
                    .permitIf(ServerSideStates.STOPPED, BasicMachineStates.STOPPED, () -> checkForStoppedTransition())
                    .permit(PlotterTriggers.STOP_DONE, BasicMachineStates.STOPPED);

            configure(BasicMachineStates.STOPPED)
                    .permit(PlotterTriggers.RESET, BasicMachineStates.RESETTING);

            configure(BasicMachineStates.RESETTING)
                    .permitIf(ConveyorStates.IDLE_EMPTY, BasicMachineStates.IDLE, () -> checkForIdleTransition())
                    .permitIf(ConveyorStates.IDLE_FULL, BasicMachineStates.IDLE, () -> checkForIdleTransition())
                    .permitIf(ServerSideStates.IDLE_EMPTY, BasicMachineStates.IDLE, () -> checkForIdleTransition())
                    .permit(PlotterTriggers.RESET_DONE, BasicMachineStates.IDLE)
                    .permit(ServerSideStates.IDLE_LOADED, BasicMachineStates.STOPPED)
                    .permit(PlotterTriggers.STOP, BasicMachineStates.STOPPING);
        }

        protected boolean checkForIdleTransition() {
            return fuStateInfo.getHandshakeFUState() == ServerSideStates.IDLE_EMPTY
                    && (fuStateInfo.getConveyorFUState() == ConveyorStates.IDLE_EMPTY ||
                    fuStateInfo.getConveyorFUState() == ConveyorStates.IDLE_FULL);
        }

        protected boolean checkForStoppedTransition() {
            return fuStateInfo.getConveyorFUState() == ConveyorStates.STOPPED
                    && fuStateInfo.getHandshakeFUState() == ServerSideStates.STOPPED;
        }

    }
}
