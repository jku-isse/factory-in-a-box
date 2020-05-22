package fiab.turntable.turning;

import static fiab.turntable.turning.TurningStates.*;
import static fiab.turntable.turning.TurningTriggers.*;

import com.github.oxo42.stateless4j.StateMachineConfig;


public class TurningStateMachineConfig extends StateMachineConfig<TurningStates, TurningTriggers> {

    public TurningStateMachineConfig() {
        configure(IDLE)
                .permit(TURN_TO, STARTING)
                .permit(STOP, STOPPING);
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
                .permit(NEXT, IDLE)
                .permit(RESET, RESETTING)
                .permit(STOP, STOPPING);
        configure(STOPPING)
                .permit(NEXT, STOPPED);
        configure(STOPPED)
                .permit(RESET, RESETTING);
        configure(RESETTING)
                .permit(NEXT, IDLE)
                .permit(STOP, STOPPING);
    }
}
