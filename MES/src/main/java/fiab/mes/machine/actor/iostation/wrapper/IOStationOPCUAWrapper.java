package fiab.mes.machine.actor.iostation.wrapper;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.opcua.AbstractOPCUAWrapper;

public class IOStationOPCUAWrapper extends AbstractOPCUAWrapper implements IOStationWrapperInterface {
	
	protected InterMachineEventBus intraMachineBus; 
	
	public IOStationOPCUAWrapper(InterMachineEventBus intraMachineBus, OpcUaClient client, NodeId capabilityImplNode,
			NodeId stopMethod, NodeId resetMethod, NodeId stateVar, ActorRef spawner) {
		super(client, capabilityImplNode,stopMethod,resetMethod,stateVar, spawner);
		this.intraMachineBus = intraMachineBus;
		logger.info("IOStationOPCUAWrapper initialized");
	}

	public void onStateSubscriptionChange(UaMonitoredItem item, DataValue value) {
		logger.debug(
				"subscription value received: item={}, value={}",
				item.getReadValueId().getNodeId(), value.getValue());
		if( value.getValue().isNotNull() ) {
			String stateAsString = value.getValue().getValue().toString();
			//System.out.println(stateAsString);
			try {
				ServerSideStates state = ServerSideStates.valueOf(stateAsString);
				if (this.intraMachineBus != null) {
					intraMachineBus.publish(new IOStationStatusUpdateEvent("", "OPCUA State Endpoint has new State", state));
				}
			} catch (java.lang.IllegalArgumentException e) {
				logger.error("Received Unknown State: "+e.getMessage());
			}
			
		}
	}

	@Override
	public void subscribeToLoadStatus() {
		// TODO Auto-generated method stub

	}

}
