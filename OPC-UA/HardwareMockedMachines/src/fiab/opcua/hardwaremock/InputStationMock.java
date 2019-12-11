package fiab.opcua.hardwaremock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.processing.Generated;

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

import fiab.opcua.hardwaremock.methods.BlankMethod;
import fiab.opcua.hardwaremock.methods.Methods;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ulong;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

public class InputStationMock extends ManagedNamespace implements Runnable{

	private UaFolderNode rootNode = null;
	private OpcUaServer server;
	private Methods method;
	
//	public static void main(String args[]) throws Exception{
//		BaseOpcUaServer server1 = new BaseOpcUaServer(0);
//		InputStationMock ism1 = new InputStationMock(server1.getServer(), NAMESPACE_URI);
//		BaseOpcUaServer server2 = new BaseOpcUaServer(1);
//		InputStationMock ism2 = new InputStationMock(server2.getServer(), NAMESPACE_URI);
//		//differentiate in/out
//		Thread s1 = new Thread(ism1);
//		Thread s2 = new Thread(ism2);
//		s1.start();
//		s2.start();
//	}
	
	public void run() {
		
		startup();
		UaFolderNode handshakeNode = generateFolder(rootNode, "InputStationMachine", "HANDSHAKE_FU");
		addMethodNode(handshakeNode, "InputStationMachine/HANDSHAKE_FU", "COMPLETE", new BlankMethod());
		addMethodNode(handshakeNode, "InputStationMachine/HANDSHAKE_FU", "STOP", method);
		addMethodNode(handshakeNode, "InputStationMachine/HANDSHAKE_FU", "RESET", new BlankMethod());
		addMethodNode(handshakeNode, "InputStationMachine/HANDSHAKE_FU", "READY", new BlankMethod());
		addMethodNode(handshakeNode, "InputStationMachine/HANDSHAKE_FU", "INIT_HANDOVER", new BlankMethod());
		addMethodNode(handshakeNode, "InputStationMachine/HANDSHAKE_FU", "START_HANDOVER", new BlankMethod());
		UaFolderNode capabilitiesFolder = generateFolder(handshakeNode, "InputStationMachine/HANDSHAKE_FU", new String("CAPABILITIES"));
		UaFolderNode capability1 = generateFolder(capabilitiesFolder, "InputStationMachine/HANDSHAKE_FU/CAPABILITIES", "CAPABILITY");
		UaFolderNode capability2 = generateFolder(capabilitiesFolder, "InputStationMachine/HANDSHAKE_FU/CAPABILITIES", "CAPABILITY");
		generateStateVariableNode(capability1, "InputStationMachine/HANDSHAKE_FU/CAPABILITIES/CAPABILITY", "ID", new String("DefaultHandshake"));
		generateStateVariableNode(capability1, "InputStationMachine/HANDSHAKE_FU/CAPABILITIES/CAPABILITY", "TYPE", new String("DEFAULT"));
		generateStateVariableNode(capability1, "InputStationMachine/HANDSHAKE_FU/CAPABILITIES/CAPABILITY", "Provided", true);
		generateStateVariableNode(capability2, "InputStationMachine/HANDSHAKE_FU/CAPABILITIES/CAPABILITY", "TYPE", new String("http://fiab.actors/InputStation"));
		generateStateVariableNode(handshakeNode, "InputStationMachine/HANDSHAKE_FU", "STATE", new String("READY"));
		try {
			server.startup().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final CompletableFuture<Void> future = new CompletableFuture<>();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));

		try {
			future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static final String NAMESPACE_URI = "urn:factory-in-a-box";
	private final SubscriptionModel subscriptionModel;

	public InputStationMock(OpcUaServer server, String namespaceUri, Methods method) {
		super(server, namespaceUri);
		this.method = method;
		this.server = server;
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
		
		//TODO check if necessary
//		generateCapabilitiesFolder(folderNode, "InputStationMachine", "CAPABILITIES");
//		
//		generateStateVariableNode(folderNode, "InputStationMachine", "STATE");
		
		rootNode = folderNode;
	}
	 
	private UaFolderNode generateFolder(UaFolderNode rootNode, String nodeIdPrefix, String folderName) {
		
		UaFolderNode folder = new UaFolderNode(
	            getNodeContext(),
	            newNodeId(nodeIdPrefix+"/" + folderName),
	            newQualifiedName(folderName), //add different id
	            LocalizedText.english(folderName)
	        );

	        getNodeManager().addNode(folder);
	        rootNode.addOrganizes(folder);		
	        return folder;
	}
	
	private void addMethodNode(UaFolderNode folderNode, String nodeIdPrefix, String name, Methods method) {
        UaMethodNode methodNode = UaMethodNode.builder(this.getNodeContext())
            .setNodeId(newNodeId(nodeIdPrefix + "/" + name))
            .setBrowseName(newQualifiedName(name))
            .setDisplayName(new LocalizedText(null, name))
            .setDescription(
                LocalizedText.english("Prints first letter of a string"))
            .build();


        try {
            AnnotationBasedInvocationHandler invocationHandler =
                AnnotationBasedInvocationHandler.fromAnnotatedObject(
                   	this.getServer(), method);

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            getNodeManager().addNode(methodNode);

            folderNode.addReference(new Reference(
                folderNode.getNodeId(),
                Identifiers.HasComponent,
                methodNode.getNodeId().expanded(),
                methodNode.getNodeClass(),
                true
            ));

            methodNode.addReference(new Reference(
                methodNode.getNodeId(),
                Identifiers.HasComponent,
                folderNode.getNodeId().expanded(),
                folderNode.getNodeClass(),
                false
            ));
            folderNode.addOrganizes(methodNode);
        } catch (Exception e) {
           	System.out.println("Error occured during creation of " + name + " method!");
        }
    }
	
	private void generateStateVariableNode(UaFolderNode rootFolder, String nodeIdPrefix, String varName, Object value) {
        /*String name = "State";*/ String name = varName;
		UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId(nodeIdPrefix+"/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            node.setValue(new DataValue(new Variant(value)));
            

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
