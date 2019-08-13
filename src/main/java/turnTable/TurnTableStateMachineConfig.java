package turnTable;

import com.github.oxo42.stateless4j.StateMachineConfig;

import static turnTable.TurnTableStates.*;
import static turnTable.TurnTableTriggers.NEXT;
import static turnTable.TurnTableTriggers.START;

public class TurnTableStateMachineConfig extends StateMachineConfig<TurnTableStates, TurnTableTriggers> {

    public TurnTableStateMachineConfig(){
        configure(IDLE).permit(START, EXECUTING);
        configure(EXECUTING).permit(NEXT, IDLE);
    }
}
