package fiab.opcua.hardwaremock.turntable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import fiab.mes.mockactors.iostation.MockIOStationWrapperDelegate;
import fiab.mes.opcua.OPCUACapabilitiesWellknownBrowsenames;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.opcua.hardwaremock.MockMethod;
import fiab.opcua.hardwaremock.StatePublisher;
import fiab.opcua.hardwaremock.iostation.methods.InitHandover;
import fiab.opcua.hardwaremock.methods.CompleteMethod;
import fiab.opcua.hardwaremock.methods.InitHandoverMethod;
import fiab.opcua.hardwaremock.methods.Methods;
import fiab.opcua.hardwaremock.methods.ResetMethod;
import fiab.opcua.hardwaremock.methods.StartHandoverMethod;
import fiab.opcua.hardwaremock.methods.StopMethod;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

public class OPCUABase extends ManagedNamespace implements Runnable {

	private UaFolderNode rootNode = null;
	private OpcUaServer server;		
	private String machineName;
	private static final Logger log = LoggerFactory.getLogger(OPCUABase.class);
	
	static final String NAMESPACE_URI = "urn:factory-in-a-box";
	private final SubscriptionModel subscriptionModel;

	public OPCUABase(OpcUaServer server, String namespaceUri, String machineName) {
		super(server, namespaceUri);
		this.server = server;
		subscriptionModel = new SubscriptionModel(server, this);
		this.machineName = machineName;		
	}
	
	public UaFolderNode  prepareRootNode() {
		startup();
		return rootNode;
	}
	
	public void run() {
		setUpServerStructure(); //All folders, nodes and Methods set in the SetUp Method
		try {
			server.startup().get();
//			actor.tell(this, ActorRef.noSender());
//			actor.tell(MessageTypes.SubscribeToStateUpdates, ActorRef.noSender());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		final CompletableFuture<Void> future = new CompletableFuture<>();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));
		
		try {
			future.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
	}
	
	public void setUpServerStructure() {
		
	}

	

	@Override
	protected void onStartup() {
		super.onStartup();

		// Create a root folder and add it to the node manager
		NodeId folderNodeId = newNodeId(machineName);

		UaFolderNode folderNode = new UaFolderNode(getNodeContext(), folderNodeId,
				newQualifiedName(machineName), LocalizedText.english(machineName));

		getNodeManager().addNode(folderNode);

		// Make sure our new folder shows up under the server's Objects folder.
		folderNode.addReference(new Reference(folderNode.getNodeId(), Identifiers.Organizes,
				Identifiers.ObjectsFolder.expanded(), false));

		rootNode = folderNode;
		//actor.tell(this, ActorRef.noSender());
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

	public UaFolderNode generateFolder(UaFolderNode rootNode, String nodeIdPrefix, String folderName) {
		return generateFolder(rootNode, nodeIdPrefix, folderName, folderName);
	}

	public UaFolderNode generateFolder(UaFolderNode rootNode, String nodeIdPrefix, String folderId, String folderName) {

		UaFolderNode folder = new UaFolderNode(getNodeContext(), newNodeId(nodeIdPrefix + "/" + folderId),
				newQualifiedName(folderName), // add different id
				LocalizedText.english(folderName));

		getNodeManager().addNode(folder);
		rootNode.addOrganizes(folder);
		return folder;
	}
		
	public UaMethodNode createPartialMethodNode(String nodeIdPrefix, String id, String info) {
		UaMethodNode methodNode = UaMethodNode.builder(this.getNodeContext())
				.setNodeId(newNodeId(nodeIdPrefix + "/" + id))
				.setBrowseName(newQualifiedName(id))
				.setDisplayName(new LocalizedText(null, id))
				.setDescription(LocalizedText.english(info)).build();
		return methodNode;
	}
	
	public void addMethodNode(UaFolderNode folderNode, UaMethodNode methodNode, AbstractMethodInvocationHandler method) {		        
        methodNode.setProperty(UaMethodNode.InputArguments, method.getInputArguments());
        methodNode.setProperty(UaMethodNode.OutputArguments, method.getOutputArguments());
        methodNode.setInvocationHandler(method);

        getNodeManager().addNode(methodNode);

        methodNode.addReference(new Reference(
            methodNode.getNodeId(),
            Identifiers.HasComponent,
            folderNode.getNodeId().expanded(),
            false
        ));
    }
	
	public UaVariableNode generateStringVariableNode(UaFolderNode rootFolder, String nodeIdPrefix, String varName, Object value) {
		/* String name = "State"; */ String name = varName;
		UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
				.setNodeId(newNodeId(nodeIdPrefix + "/" + name))
				.setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
				.setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
				.setBrowseName(newQualifiedName(name)).setDisplayName(LocalizedText.english(name))
				.setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();

		node.setValue(new DataValue(new Variant(value.toString())));
		getNodeManager().addNode(node);
		rootFolder.addOrganizes(node);
		return node;
	}
	
}
