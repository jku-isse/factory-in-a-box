package fiab.opcua.wiring.utils;

import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.ID;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_REQUIRED;
import static fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE;

import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;

import fiab.opcua.CapabilityImplementationMetadata;
import fiab.opcua.CapabilityImplementationMetadata.MetadataInsufficientException;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;

public class CapabilityDiscovery {

	
	private List<CapabilityImplInfoExt> caps = new LinkedList<>();
	private String endpointUrl;
	private OpcUaClient client;
		
	public CapabilityDiscovery(String endpointUrl, OpcUaClient client) {
		super();
		this.endpointUrl = endpointUrl;
		this.client = client;
	}


	public List<CapabilityImplInfoExt> discoverAll() {
		getActorCapabilities(Identifiers.RootFolder);
		return caps;
	}
	
	
	private void getActorCapabilities(NodeId rootNode) {		
		try {
			List<Node> nodes = client.getAddressSpace().browse(rootNode).get();
			for (Node n : nodes) {
			//	log.info("Checking node: "+n.getNodeId().get().toParseableString());
				if (n instanceof UaObjectNode) {
					if (isCapabilitiesFolder(n)) {	// then the rootNode is the actorNode				
						browseCapabilitiesFolder(client, n, rootNode);						
					} else {		
						getActorCapabilities(n.getNodeId().get());
					}
				}
			}			
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
	
	private boolean isCapabilitiesFolder(Node n) throws InterruptedException, ExecutionException {
		QualifiedName bName = n.getBrowseName().get();				
		if (bName.getName().equalsIgnoreCase(CAPABILITIES)) {
			System.out.println("Found Capabilities Node with id: "+n.getNodeId().get().toParseableString());
			return true;			
		} else
			return false;
	}
	
	private void browseCapabilitiesFolder(OpcUaClient client, Node node, NodeId actorNode) throws InterruptedException, ExecutionException {
		NodeId browseRoot = node.getNodeId().get();
		List<Node> nodes = client.getAddressSpace().browse(browseRoot).get();			
		for (Node n : nodes) {
			if (n instanceof UaObjectNode) {
				if (isCapabilityFolder(n)) {
					try {
						CapabilityImplementationMetadata capMeta = getCapabilityURI(client, n);
						System.out.println("Found: "+capMeta.toString());
						caps.add(new CapabilityImplInfoExt(client, endpointUrl, actorNode, browseRoot, capMeta));						
					} catch (MetadataInsufficientException e) {
						System.out.println("Ignoring Capability Implementation information due to insufficient child fields in OPC-UA Node "+node.getNodeId().get().toParseableString());
						// continue searching for others
					}									
				} else {				
					// we stop looking here, as there should not be anything inside the capabilities node hierarchy aside from capability definitions
				}
			}
		}
	}
	
	private boolean isCapabilityFolder(Node n) throws InterruptedException, ExecutionException {
		String bName = n.getBrowseName().get().getName();
		//if (bName.equalsIgnoreCase(CAPABILITY)) {
		if (bName.startsWith(CAPABILITY)) { // currently FORTE/4DIAC cannot have two sibling nodes with the same browsename, thus there are numbers prepended which we ignore here
			System.out.println("Found Capability Node with id: "+n.getNodeId().get().toParseableString());
			return true;			
		} else
			return false;
	}

	private CapabilityImplementationMetadata getCapabilityURI(OpcUaClient client, Node node) throws InterruptedException, ExecutionException, MetadataInsufficientException  {
		List<Node> nodes = client.getAddressSpace().browse(node.getNodeId().get()).get();
		ProvOrReq provOrReq = null;
		String implId = null;
		String uri = null;
		for (Node n : nodes) {						
			if (n instanceof UaVariableNode) {
				UaVariableNode var = (UaVariableNode) n;
				String type = n.getBrowseName().get().getName();
				switch (type) {
				case ID:
					implId = (String) var.getValue().get();
					break;
				case TYPE:
					uri = var.getValue().get().toString();					
					break;
				case ROLE:
					String role = (String) var.getValue().get();
					if (role.equalsIgnoreCase(ROLE_VALUE_PROVIDED))
						provOrReq = ProvOrReq.PROVIDED;
					else if (role.equalsIgnoreCase(ROLE_VALUE_REQUIRED))
						provOrReq = ProvOrReq.REQUIRED;
					else
						System.out.println("Discovered Role does not match Required or Provided in capability "+node.getNodeId().get().toParseableString());
					// we assume provided
					provOrReq = ProvOrReq.PROVIDED;
					break;				
				}
			}			
		}
		CapabilityImplementationMetadata capMeta = new CapabilityImplementationMetadata(implId, uri, provOrReq);
		return capMeta;
	}
}
