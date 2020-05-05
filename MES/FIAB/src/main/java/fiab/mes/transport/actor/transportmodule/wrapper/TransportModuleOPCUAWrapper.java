package fiab.mes.transport.actor.transportmodule.wrapper;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.opcua.AbstractOPCUAWrapper;
import fiab.mes.transport.msg.InternalTransportModuleRequest;

public class TransportModuleOPCUAWrapper extends AbstractOPCUAWrapper implements TransportModuleWrapperInterface {

	protected NodeId transportMethod;
	
	public TransportModuleOPCUAWrapper(InterMachineEventBus intraMachineBus, OpcUaClient client,
			NodeId capabilityImplNode, NodeId stopMethod, NodeId resetMethod, NodeId stateVar, NodeId transportMethod) {
		super(intraMachineBus, client, capabilityImplNode, stopMethod, resetMethod, stateVar);
		this.transportMethod = transportMethod;
	}

	
	@Override
	public void transport(InternalTransportModuleRequest req) {
		Variant[] inputArgs = new Variant[]{new Variant(req.getCapabilityInstanceIdFrom()),
				new Variant(req.getCapabilityInstanceIdTo()),
				new Variant(req.getOrderId()),
				new Variant(req.getRequestId())};
		callMethod(transportMethod, inputArgs);
	}

	public void onStateSubscriptionChange(UaMonitoredItem item, DataValue value) {
		logger.info(
				"subscription value received: item={}, value={}",
				item.getReadValueId().getNodeId(), value.getValue());
		if( value.getValue().isNotNull() ) {
			String stateAsString = value.getValue().getValue().toString();
			System.out.println(stateAsString);
			try {
				BasicMachineStates state = BasicMachineStates.valueOf(stateAsString);
				if (this.intraMachineBus != null) {
					intraMachineBus.publish(new MachineStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "TransportModule published new State", state));
				}
			} catch (java.lang.IllegalArgumentException e) {
				logger.error("Received Unknown State: "+e.getMessage());
			}
			
		}
	}

	@Override
	public void unsubscribeFromStatus() {
		super.unsubscribeAll();
	}


}
