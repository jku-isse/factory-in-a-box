package fiab.turntable.statemachine.process;

public enum ProcessTriggers {

    TURN_TO_SOURCE, DO_HANDSHAKE_SOURCE, CONVEY_SOURCE,
    TURN_TO_DESTINATION, DO_HANDSHAKE_DESTINATION, CONVEY_DESTINATION,
    FINISH_PROCESS, //This trigger marks the process as finished
    STOP_PROCESS,   //Stop process should allow transition from all states
    CLEAR_PROCESS   //So we can reach NoProc from Done and Aborted state
}
