package fiab.opcua.client;

import com.google.common.collect.Lists;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.observer.FUStateChangedSubject;
import fiab.functionalunit.observer.FUStateObserver;
import org.eclipse.milo.opcua.sdk.client.AddressSpace;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.client.UaStackClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class FiabOpcUaClient extends OpcUaClient implements FUStateChangedSubject {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final List<FUStateObserver> observers;

    /**
     * Extension of the OpcUaClient. It provides some easy to use methods specific to this project
     */
    public FiabOpcUaClient(OpcUaClientConfig config, UaStackClient stackClient) {
        super(config, stackClient);
        observers = new ArrayList<>();
    }

    /**
     * Factory method that creates a client based on a config.
     *
     *
     * @param config containing securityTempDir, endpoints, certificates, etc.
     * @return new Client
     * @throws Exception client could not be created
     */
    static FiabOpcUaClient createFIABClient(OpcUaClientConfig config) throws Exception {
        UaStackClient stackClient = UaStackClient.create(config);
        return new FiabOpcUaClient(config, stackClient);
    }

    /**
     * Workaround. Connects the client, but returns a FiabOpcUaClient instead of UaClient in the CompletableFuture
     *
     * @return completable future with fiab client
     */
    public CompletableFuture<FiabOpcUaClient> connectFIABClient() {
        return super.connect().thenApply(c -> FiabOpcUaClient.this);
    }

    /**
     * Use this method to find the parent of a child node
     *
     * @param nodeId NodeId of the child node
     * @return NodeId of the parent
     * @throws UaException nodeId invalid or node has no parent
     */
    public NodeId getParentNodeId(NodeId nodeId) throws Exception {
        if (!nodeExists(nodeId)) {
            throw new UnsupportedOperationException("Node with id " + nodeId + " cannot be found");
        }
        return getAddressSpace()
                .browseNodes(nodeId, AddressSpace.BrowseOptions
                        .builder().
                        setBrowseDirection(BrowseDirection.Inverse)
                        .build())
                .stream().findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Could not find parent of nodeId " + nodeId))
                .getNodeId();//Throws IndexOutOfBoundsException when no parent node found
    }

    /**
     * Searches the entire address space for a node that matches the browseName starting from the Objects (!) folder
     * Be aware that this operation might take a while to complete, therefore it can block for a long time
     *
     * @param browseName browse name of the node
     * @return nodeId of the node
     */
    public NodeId getNodeIdForBrowseName(String browseName) {
        NodeId result = browseRecursive(Identifiers.ObjectsFolder, browseName);
        if (result == null) {
            throw new NoSuchElementException("Could not find node for browseName " + browseName);
        }
        return result;
    }

    private NodeId browseRecursive(NodeId root, String browseName) {
        try {
            NodeId result = null;
            List<? extends UaNode> nodes = getAddressSpace().browseNodes(root, AddressSpace.BrowseOptions.builder()
                    .setBrowseDirection(BrowseDirection.Forward)
                    .build());
            for (UaNode node : nodes) {
                if (browseName.equals(node.getBrowseName().getName())) {
                    log.info("Found node for browseName " + browseName + ": " + node.getNodeId());
                    return node.getNodeId();
                } else {
                    if (Objects.requireNonNull(node.getBrowseName().getName()).equalsIgnoreCase("Objects") ||
                            node.getNodeId().getNamespaceIndex().intValue() > 0) {
                        //We skip this node for performance reasons, might be incompatible with 4diac
                        result = browseRecursive(node.getNodeId(), browseName);
                    }

                }
            }
            return result;
        } catch (UaException e) {
            log.error("Failed while browsing nodeId " + root + " for node with browseName " + browseName);
        }
        return null;
    }

    /**
     * Searches for a child node starting at given nodeId that matches a known browseName
     * In case two child nodes have the same browse name, the first match will be chosen
     *
     * @param rootNode            nodeId of the root node the search should start at
     * @param childNodeBrowseName browseName of the child node
     * @return childNode that matches requested browseName
     * @throws UaException when rootNode invalid or NoSuchElementException when no match was found
     */
    public NodeId getChildNodeByBrowseName(NodeId rootNode, String childNodeBrowseName) throws UaException {
        return getAddressSpace()
                .browseNodes(rootNode, AddressSpace.BrowseOptions
                        .builder().setBrowseDirection(BrowseDirection.Forward)
                        .build())
                .stream()
                .filter(uaNode -> childNodeBrowseName.equals(uaNode.getBrowseName().getName()))
                .findFirst()
                .orElseThrow(() ->
                        new NoSuchElementException("Could not find " + childNodeBrowseName + " for root " + rootNode))
                .getNodeId();
    }

    /**
     * Checks whether the node is present on the server and browse name can be read.
     *
     * @param nodeId nodeId
     * @return node is present and browse name can be read
     */
    public boolean nodeExists(NodeId nodeId) {
        try {
            return getAddressSpace()
                    .getNode(nodeId).readBrowseName().isNotNull();
        } catch (UaException e) {
            log.warn("Could not find the requested node for NodeId " + nodeId + ". StatusCode: " + e.getStatusCode());
            return false;
        }
    }

    /**
     * Reads a variable node and returns the value as a string
     *
     * @param nodeId OpcUa NodeId of the variable to read
     * @return value as String
     * @throws UaException Node not found
     */
    public String readStringVariableNode(NodeId nodeId) throws UaException {
        return getAddressSpace()
                .getVariableNode(nodeId)
                .readValue()
                .getValue().getValue()   // Variant->Object
                .toString();
    }

    /**
     * An async opcua method call that returns a string value.
     *
     * @param methodId  OpcUa NodeId of the method to call
     * @param inputArgs Optional input parameters
     * @return result as CompletableFuture of String, returns empty String for void methods
     * @throws UaException node not found or method call fails
     */
    public CompletableFuture<String> callStringMethod(NodeId methodId, Variant... inputArgs) throws Exception {
        NodeId parentId = getParentNodeId(methodId);
        //If we use the NodeId directly, the client has no reference to it and fails to find Node -> method call fails!
        NodeId methodNodeId = getNodeForId(methodId).getNodeId();
        CallMethodRequest request = new CallMethodRequest(parentId, methodNodeId, inputArgs);
        return call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();
            log.info("Method call on node" + methodId + " returned statusCode " + statusCode);
            if (statusCode.isGood()) {
                //Null check to ensure compatibility with 4diac opcua stack
                if (result.getOutputArguments() != null && result.getOutputArguments().length > 0) {
                    String value = (String) (result.getOutputArguments())[0].getValue();
                    log.info("Method call on node " + methodId + " returned value: " + value);
                    return CompletableFuture.completedFuture(value);
                }
                return CompletableFuture.completedFuture("");
            } else {
                log.warn("Method call on node " + methodId + " was cancelled or failed");
                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }

    /**
     * Use this method to call a method with no args using a NodeId. This method is blocking
     *
     * @param methodId OpcUa NodeId of the method to call
     * @return result as String
     * @throws Exception node not found or method call fails
     */
    public String callStringMethodBlocking(NodeId methodId) throws Exception {
        return callStringMethodBlocking(methodId, new Variant[]{});
    }

    /**
     * Calls a method using a methodNodeId and returns the result as a String. This method is blocking.
     * Use this method to skip creating new Variants for String inputs
     *
     * @param methodId  OpcUa NodeId of the method to call
     * @param inputArgs the input arguments as String array
     * @return result as String
     * @throws Exception node not found or method call fails
     */
    public String callStringMethodBlocking(NodeId methodId, String... inputArgs) throws Exception {
        Variant[] args = Arrays.stream(inputArgs).map(arg -> new Variant(arg)).toArray(Variant[]::new);
        return callStringMethodBlocking(methodId, args);
    }

    /**
     * Use this method for testing. Use callStringMethod(nodeId, ...args) for the implementation instead.
     * During testing we want to call this method, since it will block until the method has succeeded.
     * After the call we are then free to read any variable nodes to see if the state has changed
     *
     * @param methodId  OpcUa NodeId of the method to call
     * @param inputArgs the input arguments as Variant array
     * @return result as string
     * @throws Exception node not found or method call fails
     */
    public String callStringMethodBlocking(NodeId methodId, Variant... inputArgs) throws Exception {
        return callStringMethod(methodId, inputArgs).get();
    }

    /**
     * Tries converting the nodeId to a UaNode
     *
     * @param nodeId the requested NodeId
     * @return UaNode
     * @throws UaException Error creating the Node
     */
    public UaNode getNodeForId(NodeId nodeId) throws UaException {
        return getAddressSpace().getNode(nodeId);
    }

    /**
     * Subscribes to a String variable and monitors its value.
     * The client supports the observer pattern and notifies all subscribers when the value has changed
     *
     * @param nodeId of the node we want to subscribe to
     */
    public void subscribeToStatus(NodeId nodeId) {
        // from: https://github.com/eclipse/milo/blob/release/0.3.7/milo-examples/client-examples/src/main/java/org/eclipse/milo/examples/client/SubscriptionExample.java
        try {
            UaSubscription subscription = getSubscriptionManager().createSubscription(100.0).get();
            ReadValueId readValueId = new ReadValueId(nodeId, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
            UInteger clientHandle = subscription.nextClientHandle();
            MonitoringParameters parameters = new MonitoringParameters(
                    clientHandle,
                    100.0,     // sampling interval
                    null,       // filter, null means use default
                    uint(10),   // queue size
                    true        // discard oldest
            );
            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                    readValueId,
                    MonitoringMode.Reporting,
                    parameters
            );
            UaSubscription.ItemCreationCallback onItemCreated =
                    (item, id) -> item.setValueConsumer(this::onStateSubscriptionChange);

            List<UaMonitoredItem> items = subscription.createMonitoredItems(
                    TimestampsToReturn.Both,
                    Lists.newArrayList(request),
                    onItemCreated
            ).get();
            for (UaMonitoredItem item : items) {
                if (item.getStatusCode().isGood()) {
                    log.info("item created for nodeId={}", item.getReadValueId().getNodeId());
                } else {
                    log.warn("failed to create item for nodeId={} (status={})",
                            item.getReadValueId().getNodeId(), item.getStatusCode());
                }
            }
            //isSubscribed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clears all subscriptions. The client will stop notifying subscribers until a new subscription is requested
     */
    public void unsubscribeFromStatus() {
        getSubscriptionManager().clearSubscriptions();
        log.info("Cleared Subscriptions");
    }

    /**
     *
     */
    public void disconnectClient(){
        try {
            disconnect().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while disconnecting client, please check the stackTrace for possible causes");
            e.printStackTrace();
        }
    }

    /**
     * Converts the new value to string and notifies subscribers
     *
     * @param item  item that changed
     * @param value updated value
     */
    private void onStateSubscriptionChange(UaMonitoredItem item, DataValue value) {
        log.info("subscription value received: item={}, value={}",
                item.getReadValueId().getNodeId(), value.getValue());
        if (value.getValue().isNotNull()) {
            String stateAsString = value.getValue().getValue().toString();
            System.out.println(stateAsString);
            try {
                ServerSideStates state = ServerSideStates.valueOf(stateAsString);
                notifySubscribers(state);
            } catch (java.lang.IllegalArgumentException e) {
                log.error("Received Unknown State: " + e.getMessage());
            }
        }
    }

    @Override
    public void addSubscriber(FUStateObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void removeSubscriber(FUStateObserver observer) {
        this.observers.remove(observer);
    }

    @Override
    public void notifySubscribers(Object state) {
        for (FUStateObserver observer : observers) {
            observer.notifyAboutStateChange(state);
        }
    }

}
