package fiab.handshake.fu.client;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class ServerHandshakeNodeIds {
	OpcUaClient client;
	NodeId capabilityImplNode;
	NodeId stateVar;
	NodeId initMethod; 
	NodeId startMethod;
	
	public void setClient(OpcUaClient client) {
		this.client = client;
	}
	public OpcUaClient getClient() {
		return client;
	}
	public NodeId getCapabilityImplNode() {
		return capabilityImplNode;
	}
	public void setCapabilityImplNode(NodeId capabilityImplNode) {
		this.capabilityImplNode = capabilityImplNode;
	}
	public NodeId getStateVar() {
		return stateVar;
	}
	public void setStateVar(NodeId stateVar) {
		this.stateVar = stateVar;
	}
	public NodeId getInitMethod() {
		return initMethod;
	}
	public void setInitMethod(NodeId initMethod) {
		this.initMethod = initMethod;
	}
	public NodeId getStartMethod() {
		return startMethod;
	}
	public void setStartMethod(NodeId startMethod) {
		this.startMethod = startMethod;
	} 
	
	public boolean isComplete() {
		return (capabilityImplNode != null && stateVar != null && initMethod != null && startMethod != null);
	}
	
}