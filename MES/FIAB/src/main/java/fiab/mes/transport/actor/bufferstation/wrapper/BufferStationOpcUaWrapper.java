package fiab.mes.transport.actor.bufferstation.wrapper;

import akka.actor.ActorRef;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.mes.opcua.AbstractOPCUAWrapper;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class BufferStationOpcUaWrapper extends AbstractOPCUAWrapper implements BufferStationWrapperInterface {

    protected NodeId loadMethod;
    protected NodeId unloadMethod;
    protected IntraMachineEventBus intraMachineBus;

    public BufferStationOpcUaWrapper(IntraMachineEventBus intraMachineBus, OpcUaClient client,
                                     NodeId capabilityImplNode, NodeId stopMethod, NodeId resetMethod, NodeId stateVar,
                                     NodeId loadMethod, NodeId unloadMethod, ActorRef spawner) {
        super(client, capabilityImplNode, stopMethod, resetMethod, stateVar, spawner);
        this.intraMachineBus = intraMachineBus;
        this.loadMethod = loadMethod;
        this.unloadMethod = unloadMethod;

    }

    @Override
    public void onStateSubscriptionChange(UaMonitoredItem item, DataValue value) {
        logger.debug(
                "subscription value received: item={}, value={}",
                item.getReadValueId().getNodeId(), value.getValue());
        if (value.getValue().isNotNull()) {
            String stateAsString = value.getValue().getValue().toString();
            //System.out.println(stateAsString);
            try {
                BasicMachineStates state = BasicMachineStates.valueOf(stateAsString);
                if (this.intraMachineBus != null) {
                    intraMachineBus.publish(new MachineStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "TransportModule published new State", state));
                }
            } catch (java.lang.IllegalArgumentException e) {
                logger.error("Received Unknown State: " + e.getMessage());
            }

        }
    }

    @Override
    public void load(String orderId) {
        Variant[] inputArgs = new Variant[]{new Variant(orderId)};
        callMethod(loadMethod, inputArgs);
    }

    @Override
    public void unload(String orderId) {
        Variant[] inputArgs = new Variant[]{new Variant(orderId)};
        callMethod(unloadMethod, inputArgs);
    }

    @Override
    public void unsubscribeFromStatus() {
        super.unsubscribeAll();
    }
}
