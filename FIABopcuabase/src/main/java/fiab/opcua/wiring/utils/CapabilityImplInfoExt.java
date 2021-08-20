package fiab.opcua.wiring.utils;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import fiab.opcua.CapabilityImplInfo;
import fiab.opcua.CapabilityImplementationMetadata;

public class CapabilityImplInfoExt extends CapabilityImplInfo{

	protected CapabilityImplementationMetadata metaData;
	
	public CapabilityImplInfoExt(OpcUaClient client, String endpointUrl, NodeId actorNode, NodeId capabilitiesNode,
			CapabilityImplementationMetadata metaData) {
		super(client, endpointUrl, actorNode, capabilitiesNode, metaData.getCapabilityURI());
		this.metaData = metaData;
	}

	
	
}
