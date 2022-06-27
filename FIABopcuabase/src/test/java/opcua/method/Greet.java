package opcua.method;

import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Greet extends AbstractMethodInvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final Argument GREETEE = new Argument(
            "greetee",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Name of the person that should be greeted")
    );

    public static final Argument RESPONSE = new Argument(
            "greet response",
            Identifiers.String,
            ValueRanks.Scalar,
            null,
            new LocalizedText("Hopefully a personal greet message")
    );

    public Greet(UaMethodNode methodNode) {
        super(methodNode);
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[]{GREETEE};
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[]{RESPONSE};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking test() method of objectId={}", invocationContext.getObjectId());
        return new Variant[]{new Variant("Hello " + inputValues[0].getValue().toString() + "!")};
    }
}
