package fiab.mes.transport.actor.transportmodule.wrapper;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.transport.old.TransportModuleWrapper;

public class TurntableWrapper {
	
	private TransportModuleWrapper machineWrapper;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private List<String> nodes = new ArrayList<String>();
	
	public TurntableWrapper(TransportModuleWrapper trntbl) {
		this.machineWrapper = trntbl;
		//TODO don't hardcode this!
		nodes.add(new String("Subscribe NodeId{ns=2, id=FunctionalUnit/Conveyor/ConveyorStatus}"));
	}
	
	public boolean subscribe(ActorRef actor, String nodeId) {
		String subscribeNode = "";
		//Translating the nodeId to a real server NodeId (which are stored in the nodes
		for(String node: nodes) {
			if(node.contains(nodeId)) subscribeNode = node;
		}
		logger.info(actor.toString() + " subscribed to " + subscribeNode);
		return machineWrapper.subscribe(actor, subscribeNode);
	}
	
	public void writeToServer(NodeId nodeId, DataValue value) {
		logger.info(nodeId + " updated on server to: " + value.getValue().getValue().toString());
		machineWrapper.writeToServer(nodeId, value);
	}

}
