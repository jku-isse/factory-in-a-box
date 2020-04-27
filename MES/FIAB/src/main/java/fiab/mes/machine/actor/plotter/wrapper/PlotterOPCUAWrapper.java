package fiab.mes.machine.actor.plotter.wrapper;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.opcua.AbstractOPCUAWrapper;
import fiab.mes.transport.msg.InternalTransportModuleRequest;

public class PlotterOPCUAWrapper extends AbstractOPCUAWrapper implements PlottingMachineWrapperInterface {

	protected NodeId plotMethod;
	
	public PlotterOPCUAWrapper(InterMachineEventBus intraMachineBus, OpcUaClient client,
			NodeId capabilityImplNode, NodeId stopMethod, NodeId resetMethod, NodeId stateVar, NodeId plotMethod) {
		super(intraMachineBus, client, capabilityImplNode, stopMethod, resetMethod, stateVar);
		this.plotMethod = plotMethod;
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
					intraMachineBus.publish(new MachineStatusUpdateEvent("", null, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "PlottingModule published new State", state));
				}
			} catch (java.lang.IllegalArgumentException e) {
				logger.error("Received Unknown State: "+e.getMessage());
			}			
		}
	}


	@Override
	public void plot(String imageId, String orderId) {
		// ignoring orderId
		Variant[] inputArgs = new Variant[]{new Variant(imageId)};
		callMethod(plotMethod, inputArgs);		
	}


}
