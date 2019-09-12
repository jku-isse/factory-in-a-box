package stateMachines.conveyor;

import com.github.oxo42.stateless4j.StateMachineConfig;

import static stateMachines.conveyor.ConveyorStates.FULLY_OCCUPIED;
import static stateMachines.conveyor.ConveyorStates.IDLE;
import static stateMachines.conveyor.ConveyorStates.LOADING;
import static stateMachines.conveyor.ConveyorStates.RESETTING;
import static stateMachines.conveyor.ConveyorStates.STOPPED;
import static stateMachines.conveyor.ConveyorStates.STOPPING;
import static stateMachines.conveyor.ConveyorStates.SUSPENDED;
import static stateMachines.conveyor.ConveyorStates.UNLOADING;
import static stateMachines.conveyor.ConveyorTriggers.LOAD;
import static stateMachines.conveyor.ConveyorTriggers.NEXT;
import static stateMachines.conveyor.ConveyorTriggers.NEXT_FULL;
import static stateMachines.conveyor.ConveyorTriggers.NEXT_PARTIAL;
import static stateMachines.conveyor.ConveyorTriggers.PAUSE;
import static stateMachines.conveyor.ConveyorTriggers.RESET;
import static stateMachines.conveyor.ConveyorTriggers.STOP;
import static stateMachines.conveyor.ConveyorTriggers.UNLOAD;

public class ConveyorStateMachineConfig extends StateMachineConfig<ConveyorStates, ConveyorTriggers> {

    public ConveyorStateMachineConfig(){
        configure(IDLE)
                .permit(LOAD, LOADING)
                .permitReentry(STOP)
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
                .permit(NEXT, STOPPED);
        configure(STOPPED).permit(RESET, RESETTING);
    }
}
