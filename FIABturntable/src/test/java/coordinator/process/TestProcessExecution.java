package coordinator.process;

import com.github.oxo42.stateless4j.StateMachine;
import fiab.turntable.FUStateInfo;
import fiab.turntable.statemachine.process.ProcessStateMachine;
import fiab.turntable.statemachine.process.ProcessStates;
import fiab.turntable.statemachine.process.ProcessTriggers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class TestProcessExecution {

    private StateMachine<ProcessStates, Object> process;

    @BeforeEach
    public void setup(){
        process = new ProcessStateMachine(new FUStateInfo(null));
    }

    @Test
    public void testNoProcessAsFirstState(){
        assertEquals(ProcessStates.NOPROC, process.getState());
    }

    @Test
    public void testValidTransitionsEndsInDoneState(){
        performValidTransitionsToDone();
    }

    @Test
    public void testAbortedProcessEndsInAbortedState(){
        process.fire(ProcessTriggers.TURN_TO_SOURCE);
        assertEquals(ProcessStates.TURNING_SOURCE, process.getState());

        process.fire(ProcessTriggers.DO_HANDSHAKE_SOURCE);
        assertEquals(ProcessStates.HANDSHAKE_SOURCE,process.getState());

        process.fire(ProcessTriggers.STOP_PROCESS);
        assertEquals(ProcessStates.ABORTED, process.getState());
    }

    @Test
    public void testFinishedProcessClearEndsInNoProcess(){
        performValidTransitionsToDone();

        process.fire(ProcessTriggers.CLEAR_PROCESS);
        assertEquals(ProcessStates.NOPROC, process.getState());
    }

    @Test
    public void testAbortedProcessClearEndsInNoProcess(){
        process.fire(ProcessTriggers.TURN_TO_SOURCE);
        process.fire(ProcessTriggers.STOP_PROCESS);
        assertEquals(ProcessStates.ABORTED, process.getState());

        process.fire(ProcessTriggers.CLEAR_PROCESS);
        assertEquals(ProcessStates.NOPROC, process.getState());
    }


    private void performValidTransitionsToDone(){
        process.fire(ProcessTriggers.TURN_TO_SOURCE);
        assertEquals(ProcessStates.TURNING_SOURCE, process.getState());

        process.fire(ProcessTriggers.DO_HANDSHAKE_SOURCE);
        assertEquals(ProcessStates.HANDSHAKE_SOURCE,process.getState());

        process.fire(ProcessTriggers.CONVEY_SOURCE);
        assertEquals(ProcessStates.CONVEYING_SOURCE, process.getState());

        process.fire(ProcessTriggers.TURN_TO_DESTINATION);
        assertEquals(ProcessStates.TURNING_DEST, process.getState());

        process.fire(ProcessTriggers.DO_HANDSHAKE_DESTINATION);
        assertEquals(ProcessStates.HANDSHAKE_DEST, process.getState());

        process.fire(ProcessTriggers.CONVEY_DESTINATION);
        assertEquals(ProcessStates.CONVEYING_DEST, process.getState());

        process.fire(ProcessTriggers.FINISH_PROCESS);
        assertEquals(ProcessStates.DONE, process.getState());
    }

}
