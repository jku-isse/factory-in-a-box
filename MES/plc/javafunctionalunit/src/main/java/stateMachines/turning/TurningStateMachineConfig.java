package stateMachines.turning;

import com.github.oxo42.stateless4j.StateMachineConfig;

import static stateMachines.turning.TurningStates.COMPLETE;
import static stateMachines.turning.TurningStates.COMPLETING;
import static stateMachines.turning.TurningStates.EXECUTING;
import static stateMachines.turning.TurningStates.IDLE;
import static stateMachines.turning.TurningStates.RESETTING;
import static stateMachines.turning.TurningStates.STARTING;
import static stateMachines.turning.TurningStates.STOPPED;
import static stateMachines.turning.TurningStates.STOPPING;
import static stateMachines.turning.TurningTriggers.EXECUTE;
import static stateMachines.turning.TurningTriggers.NEXT;
import static stateMachines.turning.TurningTriggers.RESET;
import static stateMachines.turning.TurningTriggers.STOP;
import static stateMachines.turning.TurningTriggers.TURN_TO;


public class TurningStateMachineConfig extends StateMachineConfig<TurningStates, TurningTriggers> {

    public TurningStateMachineConfig(){
        configure(IDLE).permit(TURN_TO, STARTING).permit(STOP, STOPPING).permitReentry(RESET).ignore(NEXT);
        configure(STARTING).permit(EXECUTE, EXECUTING).permit(STOP, STOPPING);
        configure(EXECUTING).permit(NEXT, COMPLETING).permit(STOP, STOPPING);
        configure(COMPLETING).permit(NEXT, COMPLETE).permit(STOP, STOPPING);
        configure(COMPLETE).permit(NEXT, IDLE).permit(STOP, STOPPING);
        configure(STOPPING).permit(NEXT, STOPPED);
        configure(STOPPED).permit(RESET, RESETTING);
        configure(RESETTING).permit(NEXT, IDLE).permit(STOP, STOPPING);
    }
}
