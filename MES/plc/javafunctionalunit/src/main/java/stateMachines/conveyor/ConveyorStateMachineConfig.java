package stateMachines.conveyor;

import com.github.oxo42.stateless4j.StateMachineConfig;

import static stateMachines.conveyor.ConveyorStates.*;
import static stateMachines.conveyor.ConveyorTriggers.*;

public class ConveyorStateMachineConfig extends StateMachineConfig<ConveyorStates, ConveyorTriggers> {

    public ConveyorStateMachineConfig(){
        configure(IDLE)
                .permit(LOAD, LOADING)
                .permit(STOP, STOPPING)
                .ignore(NEXT);
        configure(LOADING)
                .permit(PAUSE, SUSPENDED)
                .permit(NEXT, FULLY_OCCUPIED)
                .permit(STOP, STOPPING);
        configure(SUSPENDED)
                .permit(LOAD, LOADING)
                .permit(UNLOAD, UNLOADING)
                .permit(STOP, STOPPING);
        configure(UNLOADING)
                .permit(PAUSE, SUSPENDED)
                .permit(STOP, STOPPING)
                .permit(NEXT, IDLE);
        configure(FULLY_OCCUPIED)
                .permit(UNLOAD, UNLOADING)
                .permit(STOP, STOPPING);
        configure(RESETTING)
                .permit(NEXT_PARTIAL, SUSPENDED)
                .permit(NEXT_FULL, FULLY_OCCUPIED)
                .permit(NEXT, IDLE)
                .permit(STOP, STOPPING);
        configure(STOPPING)
                .permit(NEXT, STOPPED)
                .ignore(STOP);
        configure(STOPPED).permit(RESET, RESETTING).ignore(STOP).ignore(NEXT);
    }
}
