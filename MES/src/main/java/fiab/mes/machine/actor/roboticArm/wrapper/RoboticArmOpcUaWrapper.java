package fiab.mes.machine.actor.roboticArm.wrapper;

import akka.actor.ActorRef;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.opcua.AbstractOPCUAWrapper;
import fiab.opcua.client.FiabOpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.concurrent.ExecutionException;

public class RoboticArmOpcUaWrapper extends AbstractOPCUAWrapper implements RoboticArmWrapperInterface {

    private final NodeId pickMethod;
    private final MachineEventBus intraMachineBus;
    private final FiabOpcUaClient opcuaClient;


    public RoboticArmOpcUaWrapper(MachineEventBus intraMachineBus, OpcUaClient client, NodeId capabilityImplNode, NodeId stopMethod, NodeId resetMethod, NodeId pickMethod, NodeId stateVar, ActorRef spawner) {
        super(client, capabilityImplNode, stopMethod, resetMethod, stateVar, spawner);
        this.pickMethod = pickMethod;
        this.intraMachineBus = intraMachineBus;
        this.opcuaClient = (FiabOpcUaClient) client;
    }

    @Override
    public void pick(String partId) {
        logger.info("Invoked pick method");
        Variant[] inputArgs = new Variant[]{new Variant(partId)};
        try {
            opcuaClient.callStringMethod(pickMethod, inputArgs).thenAccept(res -> System.out.println(res));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStateSubscriptionChange(UaMonitoredItem item, DataValue value) {
        logger.debug("subscription value received: item={}, value={}", item.getReadValueId().getNodeId(), value.getValue());
        if (value.getValue().isNotNull()) {
            String stateAsString = value.getValue().getValue().toString();
            //System.out.println(stateAsString);
            try {
                if (stateAsString.contentEquals("RESET")) {
                    stateAsString = "RESETTING";
                } else if (stateAsString.contentEquals("EXECUTING")) {
                    stateAsString = "EXECUTE";
                }
                BasicMachineStates state = BasicMachineStates.valueOf(stateAsString);
                if (this.intraMachineBus != null) {
                    intraMachineBus.publish(new MachineStatusUpdateEvent("RoboticArm", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "RoboticArm published new State", state));
                }
            } catch (java.lang.IllegalArgumentException e) {
                logger.error("Received Unknown State: " + e.getMessage());
            }
        }
    }
}
