package turnTable;

import com.github.oxo42.stateless4j.StateMachineConfig;

import static turnTable.TurnTableStates.*;
import static turnTable.TurnTableTriggers.*;

public class TurnTableStateMachineConfig extends StateMachineConfig<TurnTableStates, TurnTableTriggers> {

    public TurnTableStateMachineConfig(){
        configure(IDLE).permit(START, EXECUTING);
        configure(IDLE).ignore(STOP);
        configure(EXECUTING).permit(NEXT, IDLE);
        configure(EXECUTING).permit(STOP, IDLE);
    }
}
