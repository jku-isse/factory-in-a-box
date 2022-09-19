package fiab.mes.transport.actor.transportmodule.wrapper;

import fiab.core.capabilities.transport.TransportRequest;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import akka.actor.ActorRef;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.mes.opcua.AbstractOPCUAWrapper;

public class TransportModuleOPCUAWrapper extends AbstractOPCUAWrapper implements TransportModuleWrapperInterface {

	protected NodeId transportMethod;
	protected MachineEventBus intraMachineBus;
	
	public TransportModuleOPCUAWrapper(MachineEventBus intraMachineBus, OpcUaClient client,
			NodeId capabilityImplNode, NodeId stopMethod, NodeId resetMethod, NodeId stateVar, NodeId transportMethod, ActorRef spawner) {
		super(client, capabilityImplNode, stopMethod, resetMethod, stateVar, spawner);
		this.intraMachineBus = intraMachineBus;
		this.transportMethod = transportMethod;
	}

	
	@Override
	public void transport(TransportRequest req) {
		Variant[] inputArgs = new Variant[]{new Variant(req.getCapabilityInstanceIdFrom()),
				new Variant(req.getCapabilityInstanceIdTo()),
				new Variant(req.getOrderId()),
				new Variant(req.getRequestId())};
		callMethod(transportMethod, inputArgs);
	}

	public void onStateSubscriptionChange(UaMonitoredItem item, DataValue value) {
		logger.debug(
				"subscription value received: item={}, value={}",
				item.getReadValueId().getNodeId(), value.getValue());
		if( value.getValue().isNotNull() ) {
			String stateAsString = value.getValue().getValue().toString();
			//System.out.println(stateAsString);
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
