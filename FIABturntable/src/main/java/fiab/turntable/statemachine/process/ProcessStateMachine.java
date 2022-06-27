package fiab.turntable.statemachine.process;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.observer.FUStateObserver;
import fiab.turntable.FUStateInfo;
import fiab.conveyor.statemachine.ConveyorStates;
import fiab.turntable.turning.statemachine.TurningStates;

import static fiab.turntable.statemachine.process.ProcessStates.*;
import static fiab.turntable.statemachine.process.ProcessTriggers.*;

public class ProcessStateMachine extends StateMachine<ProcessStates, Object> implements FUStateObserver{

    private FUStateInfo info;

    public ProcessStateMachine(FUStateInfo info) {
        super(NOPROC, new ProcessStateMachineConfig());
        this.info = info;
        info.addSubscriber(this);
    }

    @Override
    public void notifyAboutStateChange(Object currentState) {
        if(canFire(currentState)){
            fire(currentState);
        }
    }


    static class ProcessStateMachineConfig extends StateMachineConfig<ProcessStates, Object>  {

        ProcessStateMachineConfig(){
            configure(NOPROC).permit(TURN_TO_SOURCE, TURNING_SOURCE).ignore(CLEAR_PROCESS);
            configure(TURNING_SOURCE)
                    .permit(TurningStates.COMPLETE, HANDSHAKE_SOURCE)
                    .permit(DO_HANDSHAKE_SOURCE, HANDSHAKE_SOURCE)
                    .permit(STOP_PROCESS, ABORTED);

            configure(HANDSHAKE_SOURCE)
                    .permit(ServerSideStates.EXECUTE, CONVEYING_SOURCE)
                    .permit(ClientSideStates.EXECUTE, CONVEYING_SOURCE)
                    .permit(CONVEY_SOURCE, CONVEYING_SOURCE)
                    .permit(STOP_PROCESS, ABORTED);

            configure(CONVEYING_SOURCE)
                    .permit(ConveyorStates.IDLE_EMPTY, TURNING_DEST)
                    .permit(ConveyorStates.IDLE_FULL, TURNING_DEST)
                    .permit(TURN_TO_DESTINATION, TURNING_DEST)
                    .permit(STOP_PROCESS, ABORTED);

            configure(TURNING_DEST)
                    .permit(TurningStates.COMPLETE, HANDSHAKE_DEST)
                    .permit(DO_HANDSHAKE_DESTINATION, HANDSHAKE_DEST)
                    .permit(STOP_PROCESS, ABORTED);

            configure(HANDSHAKE_DEST)
                    .permit(ServerSideStates.EXECUTE, CONVEYING_DEST)
                    .permit(ClientSideStates.EXECUTE, CONVEYING_DEST)
                    .permit(CONVEY_DESTINATION, CONVEYING_DEST)
                    .permit(STOP_PROCESS, ABORTED);

            configure(CONVEYING_DEST)
                    .permit(ConveyorStates.IDLE_EMPTY, DONE)
                    .permit(ConveyorStates.IDLE_FULL, DONE)
                    .permit(FINISH_PROCESS, DONE) //Success
                    .permit(STOP_PROCESS, ABORTED);  //Error

            configure(DONE)
                    .permit(CLEAR_PROCESS, NOPROC);

            configure(ABORTED)
                    .permit(CLEAR_PROCESS, NOPROC);
        }
    }
}
