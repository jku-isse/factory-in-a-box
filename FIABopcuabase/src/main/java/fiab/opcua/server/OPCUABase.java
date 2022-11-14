package fiab.opcua.server;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

public class OPCUABase extends ManagedNamespaceWithLifecycle implements Runnable {

    private UaFolderNode rootNode = null;
    private final OpcUaServer server;
    private final String machineName;
    private static final Logger log = LoggerFactory.getLogger(OPCUABase.class);

    static final String NAMESPACE_URI = "urn:factory-in-a-box";
    private final SubscriptionModel subscriptionModel;

    public static OPCUABase createAndStartLocalServer(int port, String machineName) {
        OpcUaServer server = new NonEncryptionBaseOpcUaServer(port, machineName).getServer();
        OPCUABase opcuaBase = new OPCUABase(server, machineName);
        opcuaBase.prepareRootNode();
        new Thread(opcuaBase).start();
        return opcuaBase;
    }

    public static OPCUABase createAndStartDiscoverableServer(int port, String machineName) {
        try {
            OpcUaServer server = new PublicNonEncryptionBaseOpcUaServer(port, machineName).getServer();
            OPCUABase opcuaBase = new OPCUABase(server, machineName);
            opcuaBase.prepareRootNode();
            new Thread(opcuaBase).start();
            return opcuaBase;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public OPCUABase(OpcUaServer server, String machineName) {
        this(server, NAMESPACE_URI, machineName);
    }

    public OPCUABase(OpcUaServer server, String namespaceUri, String machineName) {
        super(server, namespaceUri);
        this.server = server;
        this.machineName = machineName;
        this.subscriptionModel = new SubscriptionModel(server, this);

        getLifecycleManager().addLifecycle(subscriptionModel);
        getLifecycleManager().addStartupTask(this::setUpServerStructure);
    }

    public UaFolderNode prepareRootNode() {
        super.startup();
        return rootNode;
    }

    public UaFolderNode getRootNode() {
        return rootNode;
    }

    public String getMachineName() {
        return machineName;
    }

    public void run() {
        setUpServerStructure(); //All folders, nodes and Methods set in the SetUp Method
        try {
            server.startup().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        final CompletableFuture<Void> future = new CompletableFuture<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }

    public void setUpServerStructure() {
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

    public CompletableFuture<OpcUaServer> shutDownOpcUaBaseAsync() {
        shutdown();
        return server.shutdown();
    }

    public void shutDownOpcUaBase() {
        shutdown();
        try {
            server.shutdown().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
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

    /**
     * This method adds a new ua folder to an existing opcua folder node
     * It adds the path of the root node as a nodeId prefix.
     *
     * @param rootNode   parent folder node
     * @param folderName name of the folder
     * @return new Folder as UaFolderNode
     */
    public UaFolderNode generateFolder(UaFolderNode rootNode, String folderName) {
        String nodeIdPrefix = getPrefixFromNode(rootNode);
        UaFolderNode folder = new UaFolderNode(getNodeContext(), newNodeId(nodeIdPrefix + "/" + folderName),
                newQualifiedName(folderName), // add different id
                LocalizedText.english(folderName));

        getNodeManager().addNode(folder);
        rootNode.addOrganizes(folder);
        return folder;
    }

    /**
     * Use generateFolder(UaFolderNode rootNode, String folderName) instead
     *
     * @param rootNode
     * @param nodeIdPrefix
     * @param folderName
     * @return
     */
    @Deprecated
    public UaFolderNode generateFolder(UaFolderNode rootNode, String nodeIdPrefix, String folderName) {
        return generateFolder(rootNode, nodeIdPrefix, folderName, folderName);
    }


    /**
     * Use generateFolder(UaFolderNode rootNode, String folderName) instead
     *
     * @param rootNode
     * @param nodeIdPrefix
     * @param folderId
     * @param folderName
     * @return
     */
    @Deprecated
    public UaFolderNode generateFolder(UaFolderNode rootNode, String nodeIdPrefix, String folderId, String folderName) {
        UaFolderNode folder = new UaFolderNode(getNodeContext(), newNodeId(nodeIdPrefix + "/" + folderId),
                newQualifiedName(folderName), // add different id
                LocalizedText.english(folderName));

        getNodeManager().addNode(folder);
        rootNode.addOrganizes(folder);
        return folder;
    }

    /**
     * This method creates a method node.
     * Instead of keeping track of the path it is sufficient to provide the root node where this method will be placed
     * The prefix will be automatically deduced from the folderNode
     *
     * @param rootNode parent folder node
     * @param id       id of the method, usually the method name
     * @param info     more information about the method
     * @return MethodNode
     */
    public UaMethodNode createPartialMethodNode(UaFolderNode rootNode, String id, String info) {
        String nodeIdPrefix = getPrefixFromNode(rootNode);
        return UaMethodNode.builder(this.getNodeContext())
                .setNodeId(newNodeId(nodeIdPrefix + "/" + id))
                .setBrowseName(newQualifiedName(id))
                .setDisplayName(new LocalizedText(null, id))
                .setDescription(LocalizedText.english(info)).build();
    }

    /**
     * Please use createPartialMethodNode(UaFolderNode rootNode, String id, String info) instead
     *
     * @param nodeIdPrefix
     * @param id
     * @param info
     * @return
     */
    @Deprecated
    public UaMethodNode createPartialMethodNode(String nodeIdPrefix, String id, String info) {
        UaMethodNode methodNode = UaMethodNode.builder(this.getNodeContext())
                .setNodeId(newNodeId(nodeIdPrefix + "/" + id))
                .setBrowseName(newQualifiedName(id))
                .setDisplayName(new LocalizedText(null, id))
                .setDescription(LocalizedText.english(info)).build();
        return methodNode;
    }

    public void addMethodNode(UaFolderNode folderNode, UaMethodNode methodNode, AbstractMethodInvocationHandler method) {
        methodNode.setInputArguments(method.getInputArguments());
        methodNode.setOutputArguments(method.getOutputArguments());
        methodNode.setInvocationHandler(method);

        getNodeManager().addNode(methodNode);

        methodNode.addReference(new Reference(
                methodNode.getNodeId(),
                Identifiers.HasComponent,
                folderNode.getNodeId().expanded(),
                false
        ));
    }

    /**
     * Creates a new Variable Node that holds a String value. It is added directly to the
     * folder node which is passed as the root
     *
     * @param rootFolder folder where variable should be added
     * @param varName    display name of the variable
     * @param value      initial value of the string variable
     * @return reference to newly created variable node
     */
    public UaVariableNode generateStringVariableNode(UaFolderNode rootFolder, String varName, Object value) {
        String nodeIdPrefix = getPrefixFromNode(rootFolder);
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId(nodeIdPrefix + "/" + varName))
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setUserAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(varName)).setDisplayName(LocalizedText.english(varName))
                .setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();

        node.setValue(new DataValue(new Variant(value.toString())));
        getNodeManager().addNode(node);
        rootFolder.addOrganizes(node);
        return node;
    }

    /**
     * Please use generateStringVariableNode(UaFolderNode rootFolder, String varName, Object value) instead
     *
     * @param rootFolder
     * @param nodeIdPrefix
     * @param varName
     * @param value
     * @return
     */
    @Deprecated
    public UaVariableNode generateStringVariableNode(UaFolderNode rootFolder, String nodeIdPrefix, String varName, Object value) {
        /* String name = "State"; */
        String name = varName;
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId(nodeIdPrefix + "/" + name))
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setUserAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(name)).setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType).build();

        node.setValue(new DataValue(new Variant(value.toString())));
        getNodeManager().addNode(node);
        rootFolder.addOrganizes(node);
        return node;
    }

    public String getPrefixFromNode(UaFolderNode node) {
        return node.getNodeId().getIdentifier().toString();
    }
}
