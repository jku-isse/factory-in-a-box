package fiab.mes.machine.actor.foldingstation.wrapper;

import akka.actor.ActorRef;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.machine.plotter.IntraMachineEventBus;
import fiab.mes.opcua.AbstractOPCUAWrapper;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class FoldingOPCUAWrapper extends AbstractOPCUAWrapper implements FoldingStationWrapperInterface {

    protected NodeId foldMethod;
    protected IntraMachineEventBus intraMachineBus;

    public FoldingOPCUAWrapper(IntraMachineEventBus intraMachineBus, OpcUaClient client, NodeId capabilityImplNode,
                               NodeId stopMethod, NodeId resetMethod, NodeId stateVar, NodeId foldMethod, ActorRef spawner) {
        super(client, capabilityImplNode, stopMethod, resetMethod, stateVar, spawner);
        this.foldMethod = foldMethod;
        this.intraMachineBus = intraMachineBus;
    }

    @Override
    public void fold(String shapeId, String orderId) {
        // ignoring orderId TODO?
        Variant[] inputArgs = new Variant[]{new Variant(shapeId)};
        callMethod(foldMethod, inputArgs);
    }

    @Override
    public void onStateSubscriptionChange(UaMonitoredItem item, DataValue value) {
        logger.debug(
                "subscription value received: item={}, value={}",
                item.getReadValueId().getNodeId(), value.getValue());
        if( value.getValue().isNotNull() ) {
            String stateAsString = value.getValue().getValue().toString();
            //System.out.println(stateAsString);
            try {
                if (stateAsString.contentEquals("RESET")) { // FIXME: states on plotter side not correct
                    stateAsString = "RESETTING";
                } else if (stateAsString.contentEquals("EXECUTING")) { // FIXME: states on plotter side not correct
                    stateAsString = "EXECUTE";
                }
                BasicMachineStates state = BasicMachineStates.valueOf(stateAsString);
                if (this.intraMachineBus != null) {
                    intraMachineBus.publish(new MachineStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "FoldingModule published new State", state));
                }
            } catch (java.lang.IllegalArgumentException e) {
                logger.error("Received Unknown State: "+e.getMessage());
            }
        }
    }
}
