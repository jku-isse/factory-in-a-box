package fiab.plotter.plotting.statemachine;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import fiab.core.capabilities.BasicMachineStates;
import fiab.plotter.FUStateInfo;

public class PlotterStateMachine extends StateMachine<BasicMachineStates, Object> {

    public PlotterStateMachine(FUStateInfo info) {
        super(BasicMachineStates.STOPPED, new PlotterFUStateMachineConfig(info));
    }

    static class PlotterFUStateMachineConfig extends StateMachineConfig<BasicMachineStates, Object> {

        private final FUStateInfo fuStateInfo;

        PlotterFUStateMachineConfig(FUStateInfo fuStateInfo) {
            this.fuStateInfo = fuStateInfo;

            configure(BasicMachineStates.IDLE)
                    .permit(PlotterFUTriggers.START, BasicMachineStates.STARTING)
                    .permit(PlotterFUTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.STARTING)
                    .permit(PlotterFUTriggers.EXECUTE, BasicMachineStates.EXECUTE)
                    .permit(PlotterFUTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.EXECUTE)
                    .permit(PlotterFUTriggers.COMPLETE, BasicMachineStates.COMPLETING)
                    .permit(PlotterFUTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.COMPLETING)
                    .permit(PlotterFUTriggers.COMPLETING_DONE, BasicMachineStates.COMPLETE)
                    .permit(PlotterFUTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.COMPLETE)
                    .permit(PlotterFUTriggers.RESET, BasicMachineStates.RESETTING)
                    .permit(PlotterFUTriggers.STOP, BasicMachineStates.STOPPING);

            configure(BasicMachineStates.STOPPING)
                    .permit(PlotterFUTriggers.STOP_DONE, BasicMachineStates.STOPPED);

            configure(BasicMachineStates.STOPPED)
                    .permit(PlotterFUTriggers.RESET, BasicMachineStates.RESETTING);

            configure(BasicMachineStates.RESETTING)
                    .permit(PlotterFUTriggers.RESET_DONE, BasicMachineStates.IDLE)
                    .permit(PlotterFUTriggers.STOP, BasicMachineStates.STOPPING);
        }
    }
}
