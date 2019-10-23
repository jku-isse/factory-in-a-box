package stateMachines.processEngine;

import com.github.oxo42.stateless4j.StateMachineConfig;

import static stateMachines.processEngine.ProcessEngineStates.*;
import static stateMachines.processEngine.ProcessEngineTriggers.*;

public class ProcessEngineStateMachineConfig extends StateMachineConfig<ProcessEngineStates, ProcessEngineTriggers> {

    public ProcessEngineStateMachineConfig(){
        configure(STOPPED)
                .permit(RESET, RESETTING)
                .ignore(STOP)
                .ignore(NEXT);
                //.ignore(EXECUTE);
        configure(IDLE)
                .permit(EXECUTE, EXECUTING)
                .permit(STOP, STOPPED)
                .permit(RESET, RESETTING)
                .ignore(NEXT);
        configure(EXECUTING)
                .permit(NEXT, IDLE)
                .permit(STOP, STOPPED)
                .ignore(EXECUTE)
                .ignore(RESET);
        configure(RESETTING)
                .permit(STOP, STOPPED)
                .permit(NEXT, IDLE)
                .ignore(RESET)
                .ignore(EXECUTE);
    }
}
