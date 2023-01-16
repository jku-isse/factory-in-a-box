package fiab.mes.assembly.display.msg;

import ExtensionsForAssemblyline.AssemblyHumanStep;
import akka.actor.ActorRef;
import fiab.core.capabilities.events.TimedEvent;

import java.util.List;

public class ShowNextPossibleStepsRequest extends DisplayRequest {

    private final List<AssemblyHumanStep> steps;

    public ShowNextPossibleStepsRequest(List<AssemblyHumanStep> steps, ActorRef sender) {
        super(sender);
        this.steps = steps;
    }

    public List<AssemblyHumanStep> getSteps() {
        return steps;
    }
}
