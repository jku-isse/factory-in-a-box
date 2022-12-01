package fiab.mes.assembly.display.msg;

import ExtensionsForAssemblyline.AssemblyHumanStep;
import akka.actor.ActorRef;
import fiab.core.capabilities.events.TimedEvent;

public class ShowNextStepRequest extends DisplayRequest {

    private final AssemblyHumanStep step;


    public ShowNextStepRequest(AssemblyHumanStep step, ActorRef sender) {
        super(sender);
        this.step = step;
    }

    public AssemblyHumanStep getStep() {
        return step;
    }
}
