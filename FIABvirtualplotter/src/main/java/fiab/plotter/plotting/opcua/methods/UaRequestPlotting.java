package fiab.plotter.plotting.opcua.methods;

import akka.actor.ActorRef;
import fiab.plotter.message.PlotImageRequest;
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

import java.util.Locale;

public class UaRequestPlotting extends AbstractMethodInvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ActorRef actor;

    public static final Argument IMAGE_ID = new Argument(
            "ImageId",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Image Id of the picture that should be plotted")
    );


    public UaRequestPlotting(UaMethodNode methodNode, ActorRef actor) {
        super(methodNode);
        this.actor = actor;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{IMAGE_ID};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[0];
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {

        logger.debug("Invoked TurnRequest() method of objectId={}", invocationContext.getObjectId());
        try {
            String imageId = inputValues[0].getValue().toString().toUpperCase(Locale.ROOT);
            // for now we ignore that we could have gotten a image id we don't support
            actor.tell(new PlotImageRequest(invocationContext.getObjectId().toParseableString(), imageId, "TODO"),
                    ActorRef.noSender());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return new Variant[0];
    }

}
