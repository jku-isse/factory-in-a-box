package fiab.mes.proxy.roboticArm.utils;

import fiab.core.capabilities.roboticArm.RoboticArmCapability;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.pattern.Patterns.ask;

public class MockRoboticArm {

    private final OPCUABase base;
    private UaVariableNode stateVariable;

    public MockRoboticArm(int port) {
        this.base = OPCUABase.createAndStartLocalServer(port, "Niryo");
        initServerStructure();
    }

    private void initServerStructure() {
        UaFolderNode robotNode = this.base.getRootNode();
        this.stateVariable = this.base.generateStringVariableNode(robotNode, "STATE", "IDLE");
        UaMethodNode pickNode = base.createPartialMethodNode(robotNode, "PickPart", "Picks a part");
        this.base.addMethodNode(robotNode, pickNode, new PickMethod(pickNode, this));
    }

    public void setUaState(String updatedState){
        this.stateVariable.setValue(new DataValue(new Variant(updatedState)));
        //if(updatedState.equals("COMPLETE")){    //Simulate instant auto reset.
        //    This is too fast, no subscriber update received!!!
        //    this.stateVariable.setValue(new DataValue(new Variant("IDLE")));
        //}
    }

    public void shutdown(){
        this.base.shutdown();
    }

    static class PickMethod extends AbstractMethodInvocationHandler {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final MockRoboticArm roboticArm;

        public static final Argument RESPONSE = new Argument(
                "pick response",
                Identifiers.String,
                ValueRanks.Scalar,
                null,
                new LocalizedText("Response")
        );

        public static final Argument PART_ID = new Argument(
                RoboticArmCapability.ROBOTIC_ARM_CAPABILITY_INPUT_PART_VAR_NAME,
                Identifiers.String,
                ValueRanks.Scalar,
                null,
                new LocalizedText("Part id")
        );

        /**
         * @param node the {@link UaMethodNode} this handler will be installed on.
         */
        public PickMethod(UaMethodNode node, MockRoboticArm roboticArm) {
            super(node);
            this.roboticArm = roboticArm;
        }

        @Override
        public Argument[] getInputArguments() {
            return new Argument[]{PART_ID};    //TODO
        }

        @Override
        public Argument[] getOutputArguments() {
            return new Argument[]{RESPONSE};
        }

        @Override
        protected Variant[] invoke(AbstractMethodInvocationHandler.InvocationContext invocationContext, Variant[] inputValues) throws UaException {
            logger.debug("Invoked PlotRequest() method of objectId={}", invocationContext.getObjectId());
            this.roboticArm.setUaState("COMPLETE");
            return new Variant[]{new Variant("Ok")};
        }
    }
}
