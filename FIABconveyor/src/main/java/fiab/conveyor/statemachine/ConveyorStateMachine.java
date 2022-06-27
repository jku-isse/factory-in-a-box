package fiab.conveyor.statemachine;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;

public class ConveyorStateMachine extends StateMachine<ConveyorStates, ConveyorTriggers> {

    public ConveyorStateMachine() {
        super(ConveyorStates.STOPPED, new ConveyorStateMachineConfig());
    }

    static class ConveyorStateMachineConfig extends StateMachineConfig<ConveyorStates, ConveyorTriggers> {
        ConveyorStateMachineConfig() {
            configure(ConveyorStates.IDLE_EMPTY)
                    .permit(ConveyorTriggers.LOAD, ConveyorStates.LOADING)
                    .permit(ConveyorTriggers.STOP, ConveyorStates.STOPPING);

            configure(ConveyorStates.LOADING)
                    .permit(ConveyorTriggers.LOADING_DONE, ConveyorStates.IDLE_FULL)
                    .permit(ConveyorTriggers.STOP, ConveyorStates.STOPPING);

            configure(ConveyorStates.UNLOADING)
                    .permit(ConveyorTriggers.STOP, ConveyorStates.STOPPING)
                    .permit(ConveyorTriggers.UNLOADING_DONE, ConveyorStates.IDLE_EMPTY);

            configure(ConveyorStates.IDLE_FULL)
                    .permit(ConveyorTriggers.UNLOAD, ConveyorStates.UNLOADING)
                    .permit(ConveyorTriggers.STOP, ConveyorStates.STOPPING);

            configure(ConveyorStates.RESETTING)
                    .permit(ConveyorTriggers.RESET_DONE_FULL, ConveyorStates.IDLE_FULL)
                    .permit(ConveyorTriggers.RESET_DONE_EMPTY, ConveyorStates.IDLE_EMPTY)
                    .permit(ConveyorTriggers.STOP, ConveyorStates.STOPPING);

            configure(ConveyorStates.STOPPING)
                    .permit(ConveyorTriggers.STOP_DONE, ConveyorStates.STOPPED)
                    .ignore(ConveyorTriggers.STOP);

            configure(ConveyorStates.STOPPED)
                    .permit(ConveyorTriggers.RESET, ConveyorStates.RESETTING)
                    .ignore(ConveyorTriggers.STOP);
        }
    }

    //TODO create visualisation or model from statemachine

}
