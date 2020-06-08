package fiab.mes.machine.actor.plotter.wrapper;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager.SubscriptionListener;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import akka.actor.ActorRef;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.machine.plotter.IntraMachineEventBus;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.opcua.AbstractOPCUAWrapper;

public class PlotterOPCUAWrapper extends AbstractOPCUAWrapper implements PlottingMachineWrapperInterface {

	protected NodeId plotMethod;
	protected IntraMachineEventBus intraMachineBus;
	
	
	public PlotterOPCUAWrapper(IntraMachineEventBus intraMachineBus, OpcUaClient client,
			NodeId capabilityImplNode, NodeId stopMethod, NodeId resetMethod, NodeId stateVar, NodeId plotMethod, ActorRef spawner) {
		super(client, capabilityImplNode, stopMethod, resetMethod, stateVar, spawner);
		this.plotMethod = plotMethod;
		this.intraMachineBus = intraMachineBus;			
	}
	
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
					intraMachineBus.publish(new MachineStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "PlottingModule published new State", state));
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
