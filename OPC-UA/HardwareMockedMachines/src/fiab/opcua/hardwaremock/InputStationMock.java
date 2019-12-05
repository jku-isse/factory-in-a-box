package fiab.opcua.hardwaremock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ulong;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

public class InputStationMock extends ManagedNamespace{

	
	public static void main(String args[]) throws Exception{
		BaseOpcUaServer server = new BaseOpcUaServer();

		InputStationMock ism = new InputStationMock(server.getServer(), NAMESPACE_URI);
		ism.startup();
		
		server.startup().get();

		final CompletableFuture<Void> future = new CompletableFuture<>();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));

		future.get();
	}
	
	static final String NAMESPACE_URI = "urn:factory-in-a-box";
	private final SubscriptionModel subscriptionModel;

	public InputStationMock(OpcUaServer server, String namespaceUri) {
		super(server, namespaceUri);
		subscriptionModel = new SubscriptionModel(server, this);
	}

	@Override
	protected void onStartup() {
		super.onStartup();

		// Create a root folder and add it to the node manager
		NodeId folderNodeId = newNodeId("InputStationMachine");

		UaFolderNode folderNode = new UaFolderNode(
				getNodeContext(),
				folderNodeId,
				newQualifiedName("InputStationMachine"),
				LocalizedText.english("InputStationMachine")
				);

		getNodeManager().addNode(folderNode);

		// Make sure our new folder shows up under the server's Objects folder.
		folderNode.addReference(new Reference(
				folderNode.getNodeId(),
				Identifiers.Organizes,
				Identifiers.ObjectsFolder.expanded(),
				false
				));
		
		generateCapabilitiesFolder(folderNode, "InputStationMachine");
		
		generateStateVariableNode(folderNode, "InputStationMachine");
	}
	 
	private void generateCapabilitiesFolder(UaFolderNode rootNode, String nodeIdPrefix) {
		
		UaFolderNode capabilitiesFolder = new UaFolderNode(
	            getNodeContext(),
	            newNodeId(nodeIdPrefix+"/Capabilities"),
	            newQualifiedName("Capabilities"),
	            LocalizedText.english("Capabilities")
	        );

	        getNodeManager().addNode(capabilitiesFolder);
	        rootNode.addOrganizes(capabilitiesFolder);		
	}
	
	private void generateStateVariableNode(UaFolderNode rootFolder, String nodeIdPrefix) {
        String name = "State";
		UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId(nodeIdPrefix+"/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            node.setValue(new DataValue(new Variant("STOPPED")));

           // node.setAttributeDelegate(new ValueLoggingDelegate());

            getNodeManager().addNode(node);
            rootFolder.addOrganizes(node);
	}
	
	
	@Override
	public void onDataItemsCreated(List<DataItem> dataItems) {
		subscriptionModel.onDataItemsCreated(dataItems);
	}

	@Override
	public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
	}

	@Override
	public void onDataItemsDeleted(List<DataItem> dataItems) {
		subscriptionModel.onDataItemsDeleted(dataItems);
	}

	@Override
	public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
		subscriptionModel.onMonitoringModeChanged(monitoredItems);		
	}	
	
}
