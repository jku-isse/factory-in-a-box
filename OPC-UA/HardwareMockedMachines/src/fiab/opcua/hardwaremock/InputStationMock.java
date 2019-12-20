package fiab.opcua.hardwaremock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
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

import akka.actor.ActorRef;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.opcua.hardwaremock.methods.BlankMethod;
import fiab.opcua.hardwaremock.methods.CompleteMethod;
import fiab.opcua.hardwaremock.methods.Methods;
import fiab.opcua.hardwaremock.methods.ReadyEmptyMethod;
import fiab.opcua.hardwaremock.methods.ResetMethod;
import fiab.opcua.hardwaremock.methods.StopMethod;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

public class InputStationMock extends ManagedNamespace implements Runnable {

	private UaFolderNode rootNode = null;
	private OpcUaServer server;
	private ActorRef actor;
	private UaVariableNode status = null;
	private String machineName;

	public void run() {

		startup();
		UaFolderNode handshakeNode = generateFolder(rootNode, machineName, "HANDSHAKE_FU");
		addMethodNode(handshakeNode, machineName + "/HANDSHAKE_FU", "COMPLETE", new CompleteMethod(actor));
		addMethodNode(handshakeNode, machineName + "/HANDSHAKE_FU", "STOP", new StopMethod(actor));
		addMethodNode(handshakeNode, machineName + "/HANDSHAKE_FU", "RESET", new ResetMethod(actor));
		addMethodNode(handshakeNode, machineName + "/HANDSHAKE_FU", "READY", new ReadyEmptyMethod(actor));
		addMethodNode(handshakeNode, machineName + "/HANDSHAKE_FU", "INIT_HANDOVER", new BlankMethod()); //Fragen
		addMethodNode(handshakeNode, machineName + "/HANDSHAKE_FU", "START_HANDOVER", new BlankMethod()); //Fragen
		UaFolderNode capabilitiesFolder = generateFolder(handshakeNode, machineName + "/HANDSHAKE_FU",
				new String("CAPABILITIES"));
		UaFolderNode capability1 = generateFolder(capabilitiesFolder, machineName +"/HANDSHAKE_FU/CAPABILITIES",
				"CAPABILITY1", "CAPABILITY");
		UaFolderNode capability2 = generateFolder(capabilitiesFolder, machineName +"/HANDSHAKE_FU/CAPABILITIES",
				"CAPABILITY2", "CAPABILITY");
		generateStateVariableNode(capability1, machineName +"/HANDSHAKE_FU/CAPABILITIES/CAPABILITY1", "ID",
				new String("DefaultHandshake"));
		generateStateVariableNode(capability1, machineName +"/HANDSHAKE_FU/CAPABILITIES/CAPABILITY1", "TYPE",
				new String("DEFAULT"));
		generateStateVariableNode(capability1, machineName +"/HANDSHAKE_FU/CAPABILITIES/CAPABILITY1", "Provided",
				true);
		generateStateVariableNode(capability2, machineName +"/HANDSHAKE_FU/CAPABILITIES/CAPABILITY2", "TYPE",
				new String("http://fiab.actors/" + machineName));
		generateStateVariableNode(capability2, machineName +"/HANDSHAKE_FU/CAPABILITIES/CAPABILITY2", "ID",
				new String("DefaultHandshake"));
		generateStateVariableNode(capability2, machineName +"/HANDSHAKE_FU/CAPABILITIES/CAPABILITY2", "Provided",
				true);
		status = generateStateVariableNode(handshakeNode, machineName +"/HANDSHAKE_FU", "STATE", ServerSide.Stopped); //init right
		try {
			server.startup().get();
			actor.tell(this, ActorRef.noSender());
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

	static final String NAMESPACE_URI = "urn:factory-in-a-box";
	private final SubscriptionModel subscriptionModel;

	public InputStationMock(OpcUaServer server, String namespaceUri, String machineName, ActorRef actor) {
		super(server, namespaceUri);
		this.server = server;
		this.actor = actor;
		subscriptionModel = new SubscriptionModel(server, this);
		this.machineName = machineName;
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
		actor.tell(this, ActorRef.noSender());
	}
	
	private UaFolderNode generateFolder(UaFolderNode rootNode, String nodeIdPrefix, String folderName) {
		return generateFolder(rootNode, nodeIdPrefix, folderName, folderName);
	}

	private UaFolderNode generateFolder(UaFolderNode rootNode, String nodeIdPrefix, String folderId, String folderName) {

		UaFolderNode folder = new UaFolderNode(getNodeContext(), newNodeId(nodeIdPrefix + "/" + folderId),
				newQualifiedName(folderName), // add different id
				LocalizedText.english(folderName));

		getNodeManager().addNode(folder);
		rootNode.addOrganizes(folder);
		return folder;
	}
	
	private void addMethodNode(UaFolderNode folderNode, String nodeIdPrefix, String id, Methods method) {
		addMethodNode(folderNode, nodeIdPrefix, id, id, method);
		for(int i = 0; i < 5; i++) {
			System.out.println();
		}
		System.out.println("METHOD " + id + " HAS BEEN CREATED!");
	}

	private void addMethodNode(UaFolderNode folderNode, String nodeIdPrefix, String id, String name, Methods method) {
		UaMethodNode methodNode = UaMethodNode.builder(this.getNodeContext())
				.setNodeId(newNodeId(nodeIdPrefix + "/" + id))
				.setBrowseName(newQualifiedName(name))
				.setDisplayName(new LocalizedText(null, name))
				.setDescription(LocalizedText.english(method.getInfo())).build();

		try {
			AnnotationBasedInvocationHandler invocationHandler = AnnotationBasedInvocationHandler
					.fromAnnotatedObject(this.getServer(), new MockMethod(method));

			methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
			methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
			methodNode.setInvocationHandler(invocationHandler);

			getNodeManager().addNode(methodNode);

			folderNode.addReference(new Reference(folderNode.getNodeId(), Identifiers.HasComponent,
					methodNode.getNodeId().expanded(), methodNode.getNodeClass(), true));

			methodNode.addReference(new Reference(methodNode.getNodeId(), Identifiers.HasComponent,
					folderNode.getNodeId().expanded(), folderNode.getNodeClass(), false));
			folderNode.addOrganizes(methodNode);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error occured during creation of " + name + " method!");
		}
	}

	public void setStatusValue(String newStatus) {
		if(status != null) {
			status.setValue(new DataValue(new Variant(newStatus)));
		}
	}

	private UaVariableNode generateStateVariableNode(UaFolderNode rootFolder, String nodeIdPrefix, String varName, Object value) {
		/* String name = "State"; */ String name = varName;
		UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
				.setNodeId(newNodeId(nodeIdPrefix + "/" + name))
				.setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
				.setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
				.setBrowseName(newQualifiedName(name)).setDisplayName(LocalizedText.english(name))
				.setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();

		node.setValue(new DataValue(new Variant(value.toString())));
		if (varName.equals("STATUS"))
			status = node;

		// node.setAttributeDelegate(new ValueLoggingDelegate());

		getNodeManager().addNode(node);
		rootFolder.addOrganizes(node);
		return node;
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
