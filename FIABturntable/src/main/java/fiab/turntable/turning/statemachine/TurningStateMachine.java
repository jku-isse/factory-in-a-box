package fiab.turntable.turning.statemachine;

import static fiab.turntable.turning.statemachine.TurningStates.*;
import static fiab.turntable.turning.statemachine.TurningTriggers.*;
import static fiab.turntable.turning.statemachine.TurningTriggers.COMPLETE;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;

public class TurningStateMachine extends StateMachine<TurningStates, TurningTriggers> {

    public TurningStateMachine(){
        super(STOPPED, new TurningStateMachineConfig());
    }

    public static class TurningStateMachineConfig extends StateMachineConfig<TurningStates, TurningTriggers> {

        public TurningStateMachineConfig() {
            configure(TurningStates.IDLE)
                    .permit(START, TurningStates.STARTING)
                    .permit(STOP, TurningStates.STOPPING);

            configure(TurningStates.STARTING)
                    .permit(EXECUTE, TurningStates.EXECUTING)
                    .permit(STOP, TurningStates.STOPPING);

            configure(TurningStates.EXECUTING)
                    .permit(COMPLETE, TurningStates.COMPLETING)
                    .permit(STOP, TurningStates.STOPPING);

            configure(TurningStates.COMPLETING)
                    .permit(COMPLETING_DONE, TurningStates.COMPLETE)
                    .permit(STOP, TurningStates.STOPPING);

            configure(TurningStates.COMPLETE)
                    .permit(RESET, TurningStates.RESETTING)
                    .permit(STOP, TurningStates.STOPPING);

            configure(TurningStates.STOPPING)
                    .permit(STOPPING_DONE, TurningStates.STOPPED);

            configure(TurningStates.STOPPED)
                    .permit(RESET, TurningStates.RESETTING);

            configure(TurningStates.RESETTING)
                    .permit(RESETTING_DONE, TurningStates.IDLE)
                    .permit(STOP, TurningStates.STOPPING);
        }
    }

    //TODO create visualisation or model from actual statemachine
}
