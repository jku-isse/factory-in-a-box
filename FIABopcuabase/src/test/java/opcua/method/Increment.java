package opcua.method;

import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Increment extends AbstractMethodInvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    UaVariableNode variableNode;

    public Increment(UaMethodNode methodNode, UaVariableNode node) {
        super(methodNode);
        this.variableNode = node;
    }

    @Override
    public Argument[] getInputArguments() {
        return new Argument[0];
    }

    @Override
    public Argument[] getOutputArguments() {
        return new Argument[0];
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) {
        logger.debug("Invoking test() method of objectId={}", invocationContext.getObjectId());
        int prevValue = Integer.parseInt(variableNode.getValue().getValue().getValue().toString());
        variableNode.setValue(new DataValue(new Variant(++prevValue)));
        return new Variant[0];
    }

}
