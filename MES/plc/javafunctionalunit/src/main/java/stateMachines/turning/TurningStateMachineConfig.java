package stateMachines.turning;

import com.github.oxo42.stateless4j.StateMachineConfig;

import static stateMachines.turning.TurningStates.*;
import static stateMachines.turning.TurningTriggers.*;


public class TurningStateMachineConfig extends StateMachineConfig<TurningStates, TurningTriggers> {

    public TurningStateMachineConfig() {
        configure(IDLE)
                .permit(TURN_TO, STARTING)
                .permit(STOP, STOPPING)
                .permitReentry(RESET)
                .ignore(NEXT);
        configure(STARTING)
                .permit(EXECUTE, EXECUTING)
                .permit(STOP, STOPPING);
        configure(EXECUTING)
                .permit(NEXT, COMPLETING)
                .permit(STOP, STOPPING);
        configure(COMPLETING)
                .permit(NEXT, COMPLETE)
                .permit(STOP, STOPPING);
        configure(COMPLETE)
                .permit(RESET, RESETTING)
                .permit(STOP, STOPPING);
        configure(STOPPING)
                .permit(NEXT, STOPPED)
                .ignore(STOP);
        configure(STOPPED)
                .permit(RESET, RESETTING)
                .ignore(STOP);
        configure(RESETTING)
                .permit(NEXT, IDLE)
                .permit(STOP, STOPPING);
    }
}
