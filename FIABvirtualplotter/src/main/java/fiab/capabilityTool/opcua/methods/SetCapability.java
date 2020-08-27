package fiab.capabilityTool.opcua.methods;

import akka.actor.ActorRef;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineInWrongStateResponse;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.plotting.PlotterMessageTypes;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static akka.pattern.Patterns.ask;

public class SetCapability extends AbstractMethodInvocationHandler {

    final Duration timeout = Duration.ofSeconds(2);
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ActorRef actor;

    public static final Argument RESPONSE = new Argument(
            "response",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Returns the value to be set")
    );

    public static final Argument CAPABILITY_ID = new Argument(
            "RD_1",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("ImageId that should be plotted")
    );


    public SetCapability(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode);
        this.actor = actor;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{CAPABILITY_ID};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{RESPONSE};
    }

    @Override
    protected Variant[] invoke(AbstractMethodInvocationHandler.InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        logger.debug("Invoked PlotRequest() method of objectId={}", invocationContext.getObjectId());
        Object resp;
        try {
            String capIdFrom = (String) inputValues[0].getValue();
            // for now we ignore that we could have gotten a image id we don't support
            resp = ask(actor, PlotterMessageTypes.Plot, timeout).toCompletableFuture().get();
            if (resp instanceof MachineStatusUpdateEvent) {
                resp = ((MachineStatusUpdateEvent) resp).getStatus();
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage());
            resp = BasicMachineStates.UNKNOWN;
        }
        return new Variant[]{new Variant(resp.toString())};
    }


}
