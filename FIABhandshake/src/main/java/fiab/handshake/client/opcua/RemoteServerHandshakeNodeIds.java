package fiab.handshake.client.opcua;

import fiab.opcua.client.FiabOpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

//TODO use builder design pattern or similar
public class RemoteServerHandshakeNodeIds {

	private FiabOpcUaClient client;
	private String endpoint;
	private NodeId actorNode;
	private NodeId capabilityImplNode;
	private NodeId stateVar;
	private NodeId initMethod;
	private NodeId startMethod;
	
	public void setClient(FiabOpcUaClient client) {
		this.client = client;
	}
	public FiabOpcUaClient getClient() {
		return client;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public NodeId getActorNode() {
		return actorNode;
	}

	public void setActorNode(NodeId actorNode) {
		this.actorNode = actorNode;
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