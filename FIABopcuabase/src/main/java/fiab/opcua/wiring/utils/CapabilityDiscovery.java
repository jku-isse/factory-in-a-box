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
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;

import fiab.opcua.CapabilityImplementationMetadata;
import fiab.opcua.CapabilityImplementationMetadata.MetadataInsufficientException;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

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
            List<ReferenceDescription> nodes = client.getAddressSpace().browse(rootNode);
            for (ReferenceDescription n : nodes) {
                //	log.info("Checking node: "+n.getNodeId().get().toParseableString());
                if (n.getNodeClass().equals(NodeClass.Object)) {
                    if (isCapabilitiesFolder(n)) {    // then the rootNode is the actorNode
                        browseCapabilitiesFolder(client, n, rootNode);
                    } else {
                        getActorCapabilities(n.getNodeId().toNodeIdOrThrow(client.getNamespaceTable()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isCapabilitiesFolder(ReferenceDescription n) throws InterruptedException, ExecutionException {
        QualifiedName bName = n.getBrowseName();
        if (bName != null && bName.getName().equalsIgnoreCase(CAPABILITIES)) {
            System.out.println("Found Capabilities Node with id: " + n.getNodeId().toParseableString());
            return true;
        } else
            return false;
    }

    private void browseCapabilitiesFolder(OpcUaClient client, ReferenceDescription node, NodeId actorNode) throws Exception {
        NodeId browseRoot = node.getNodeId().toNodeIdOrThrow(client.getNamespaceTable());
        List<ReferenceDescription> nodes = client.getAddressSpace().browse(browseRoot);
        for (ReferenceDescription n : nodes) {
            if (n.getNodeClass().equals(NodeClass.Object)) {
                if (isCapabilityFolder(n)) {
                    try {
                        CapabilityImplementationMetadata capMeta = getCapabilityURI(client, n);
                        System.out.println("Found: " + capMeta.toString());
                        caps.add(new CapabilityImplInfoExt(client, endpointUrl, actorNode, browseRoot, capMeta));
                    } catch (MetadataInsufficientException e) {
                        System.out.println("Ignoring Capability Implementation information due to insufficient child fields in OPC-UA Node " + node.getNodeId().toParseableString());
                        // continue searching for others
                    }
                } else {
                    // we stop looking here, as there should not be anything inside the capabilities node hierarchy aside from capability definitions
                }
            }
        }
    }

    private boolean isCapabilityFolder(ReferenceDescription n) throws InterruptedException, ExecutionException {
        String bName = n.getBrowseName().getName();
        //if (bName.equalsIgnoreCase(CAPABILITY)) {
        if (bName != null && bName.startsWith(CAPABILITY)) { // currently FORTE/4DIAC cannot have two sibling nodes with the same browsename, thus there are numbers prepended which we ignore here
            System.out.println("Found Capability Node with id: " + n.getNodeId().toParseableString());
            return true;
        } else
            return false;
    }

    private CapabilityImplementationMetadata getCapabilityURI(OpcUaClient client, ReferenceDescription node) throws Exception {
        NamespaceTable namespaceTable = client.getNamespaceTable();
        List<ReferenceDescription> nodes = client.getAddressSpace().browse(node.getNodeId().toNodeIdOrThrow(namespaceTable));
        ProvOrReq provOrReq = null;
        String implId = null;
        String uri = null;
        for (ReferenceDescription n : nodes) {
            if (n.getNodeClass().equals(NodeClass.Variable)) {
                UaVariableNode var = client.getAddressSpace().getVariableNode(n.getNodeId().toNodeIdOrThrow(namespaceTable));
                String type = n.getBrowseName().getName();
                switch (Objects.requireNonNull(type)) {
                    case ID:
                        implId = (String) var.getValue().getValue().getValue(); //DataValue->Variant->Value
                        break;
                    case TYPE:
                        uri = var.getValue().getValue().getValue().toString();
                        break;
                    case ROLE:
                        String role = (String) var.getValue().getValue().getValue().toString();
                        if (role.equalsIgnoreCase(ROLE_VALUE_PROVIDED))
                            provOrReq = ProvOrReq.PROVIDED;
                        else if (role.equalsIgnoreCase(ROLE_VALUE_REQUIRED))
                            provOrReq = ProvOrReq.REQUIRED;
                        else
                            System.out.println("Discovered Role does not match Required or Provided in capability " + node.getNodeId().toParseableString());
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
