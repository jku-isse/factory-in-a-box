package fiab.machine.plotter.opcua.methods;

import akka.actor.ActorRef;
import com.google.common.reflect.AbstractInvocationHandler;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineInWrongStateResponse;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.plotting.PlotterMessageTypes;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.machine.plotter.MachineCapabilityUpdateEvent;
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

import java.lang.reflect.Method;
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
            new LocalizedText("Response indicating (new) Plotting capability")
    );

    public static final Argument CAPABILITY = new Argument(
            WellknownPlotterCapability.PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME,    //TODO change to fitting name
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("New Capability to be set")
    );
    public SetCapability(UaMethodNode methodNode, ActorRef actor){
        super(methodNode);
        this.actor = actor;
    }
    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{CAPABILITY};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{RESPONSE};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
        logger.debug("Invoked PlotRequest() method of objectId={}", invocationContext.getObjectId());
        Object resp;
        try {
            String capIdFrom = (String) inputValues[0].getValue();
            //we ignore that we could have gotten a capability we don't support
            resp = ask(actor, PlotterMessageTypes.SetCapability, timeout).toCompletableFuture().get();
            if(resp instanceof MachineCapabilityUpdateEvent){
                resp = ((MachineCapabilityUpdateEvent) resp).getValue();
            }
            /*
            if (resp instanceof MachineStatusUpdateEvent) {
                resp = ((MachineStatusUpdateEvent) resp).getStatus();
            }
            else if (resp instanceof MachineInWrongStateResponse) {
                resp = ((MachineInWrongStateResponse) resp).getStatus();
            }*/
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage());
            resp = BasicMachineStates.UNKNOWN;
        }
        return new Variant[]{new Variant(resp.toString())};
    }
}
