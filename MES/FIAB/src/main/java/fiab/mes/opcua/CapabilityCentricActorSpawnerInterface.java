package fiab.mes.opcua;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;

public interface CapabilityCentricActorSpawnerInterface {

	public ActorRef createActorSpawner(ActorContext context);
			
	public static class SpawnRequest {
		CapabilityImplInfo info;
		
		public SpawnRequest(CapabilityImplInfo info) {
			this.info = info;
		}
		
		public CapabilityImplInfo getInfo() {
			return info;
		}
	}
	
	public static class CapabilityImplInfo {
		
		OpcUaClient client; 
		String endpointUrl;
		NodeId actorNode;
		NodeId capabilitiesNode;
		String capabilityURI;
		
		public CapabilityImplInfo(OpcUaClient client, String endpointUrl, NodeId actorNode,
				NodeId capabilitiesNode, String capabilityURI) {
			super();
			this.client = client;
			this.endpointUrl = endpointUrl;
			this.actorNode = actorNode;
			this.capabilitiesNode = capabilitiesNode;
			this.capabilityURI = capabilityURI;
		}
		
		public CapabilityImplInfo(String endpointUrl, NodeId actorNode,
				NodeId capabilitiesNode, String capabilityURI) {
			super();
			this.endpointUrl = endpointUrl;
			this.actorNode = actorNode;
			this.capabilitiesNode = capabilitiesNode;
			this.capabilityURI = capabilityURI;
		}
		
		public void setClient(OpcUaClient client) {
			this.client = client;
		}

		public OpcUaClient getClient() {
			return client;
		}

		public String getEndpointUrl() {
			return endpointUrl;
		}

		public NodeId getActorNode() {
			return actorNode;
		}

		public NodeId getCapabilitiesNode() {
			return capabilitiesNode;
		}

		public String getCapabilityURI() {
			return capabilityURI;
		};
		
		
	}
}
