package fiab.handshake.client.opcua.client;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.handshake.client.opcua.RemoteServerHandshakeNodeIds;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;


public class ClientSpawnerActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private CompletableFuture<?> clientCreationTask;
    private ActorRef sender;
    //private FiabOpcUaClient client;

    public static Props props() {
        return Props.create(ClientSpawnerActor.class, () -> new ClientSpawnerActor());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ClientSpawnerMessages.CreateNewClient.class, msg -> {    //Or something like this
                    handleCreationRequest(msg);
                })
                .match(ClientSpawnerMessages.CancelClientCreation.class, msg -> {
                    handleCancellationRequest();
                })
                /*.match(WiringRequest.class, req -> {
                    req.getInfo();
                    //TODO add browsing nodes to fiab client and apply wiring
                })*/
                .build();
    }

    private void handleCreationRequest(ClientSpawnerMessages.CreateNewClient msg) {
        sender = sender();
        clientCreationTask = createClientAsync(msg.getNodeIds().getRemoteEndpointURL())
                .thenApply(client -> {
                    RemoteServerHandshakeNodeIds nodeIds = new RemoteServerHandshakeNodeIds();
                    nodeIds.setClient(client);
                    nodeIds.setEndpoint(msg.getNodeIds().getRemoteEndpointURL());
                    nodeIds.setCapabilityImplNode(NodeId.parse(msg.getNodeIds().getRemoteNodeId()));
                    return nodeIds;
                })
                .thenApply(nodeIds -> prepareApplyingWiringInfo(nodeIds))
                .thenApply(nodeIds -> resolveCapabilityInfos(nodeIds))
                .thenAccept(nodeIds -> {
                    if (remoteHandshakeInfoComplete(nodeIds)) {
                        sender.tell(new ClientSpawnerMessages.ClientCreated(nodeIds), self());
                    } else {
                        log.warning("Remote server handshake info is not complete [" + nodeIds + "]");
                        sender.tell(new ClientSpawnerMessages.ClientCreationFailed(), self());
                    }
                })
                .exceptionally((ex) -> {
                    sender.tell(new ClientSpawnerMessages.ClientCreationFailed(), self());
                    ex.printStackTrace();
                    return null;
                });
    }

    private void handleCancellationRequest() {
        log.info("Cancelling Client creation");
        clientCreationTask.cancel(true);
        sender.tell(new ClientSpawnerMessages.ClientCreationCancelled(), self());
    }

    private CompletableFuture<FiabOpcUaClient> createClientAsync(String endpointUrl) {
        log.info("Creating client");
        CompletableFuture<FiabOpcUaClient> fiabClientTask;
        fiabClientTask = OPCUAClientFactory.createFIABClientAsync(endpointUrl);
        return fiabClientTask;
    }

    private RemoteServerHandshakeNodeIds prepareApplyingWiringInfo(RemoteServerHandshakeNodeIds nodeIds) {
        FiabOpcUaClient client = nodeIds.getClient();
        NodeId capabilityNode = nodeIds.getCapabilityImplNode();
        try {
            log.info("Searching for grandparent node of capability " + capabilityNode);
            NodeId parent = client.getParentNodeId(capabilityNode);
            log.info("Found parent node " + parent + " for Capability " + capabilityNode);
            NodeId grandparent = client.getParentNodeId(parent);
            log.info("Found Grandparent NodeId: " + grandparent + " for Capability " + capabilityNode);
            nodeIds.setActorNode(grandparent);
            return nodeIds;
        } catch (Exception e) {
            e.printStackTrace();
            return nodeIds;
        }
    }

    private RemoteServerHandshakeNodeIds resolveCapabilityInfos(RemoteServerHandshakeNodeIds nodeIds) {
        FiabOpcUaClient client = nodeIds.getClient();

        NodeId browseRoot = nodeIds.getActorNode();
        String initBrowseName = HandshakeCapability.ServerMessageTypes.RequestInitiateHandover.toString();
        String startBrowseName = HandshakeCapability.ServerMessageTypes.RequestStartHandover.toString();
        String stateBrowseName = HandshakeCapability.OPCUA_STATE_SERVERSIDE_VAR_NAME;
        try {
            NodeId initNode;
            NodeId startNode;
            try {
                initNode = client.getChildNodeByBrowseName(browseRoot, initBrowseName);
                startNode = client.getChildNodeByBrowseName(browseRoot, startBrowseName);
            } catch (NoSuchElementException nse) {
                log.info("Could not find init method, searching with 4diac compatible name instead");
                initNode = client.getChildNodeByBrowseName(browseRoot, "INIT_HANDOVER");    //forte impl
                startNode = client.getChildNodeByBrowseName(browseRoot, "START_HANDOVER");  //forte impl
                log.info("Found 4diac compatible handshake methods");
            }
            NodeId stateVar = client.getChildNodeByBrowseName(browseRoot, stateBrowseName);
            nodeIds.setInitMethod(initNode);
            nodeIds.setStartMethod(startNode);
            nodeIds.setStateVar(stateVar);
        } catch (UaException e) {
            e.printStackTrace();
        }
        return nodeIds;
    }

    public boolean remoteHandshakeInfoComplete(RemoteServerHandshakeNodeIds nodeIds) {
        boolean hasClient = nodeIds.getClient() != null;
        boolean hasEndpoint = nodeIds.getEndpoint() != null;
        boolean hasActorNode = nodeIds.getActorNode() != null;
        boolean hasCapNode = nodeIds.getCapabilityImplNode() != null;
        boolean hasStateVar = nodeIds.getStateVar() != null;
        boolean hasInitMethod = nodeIds.getInitMethod() != null;
        boolean hasStartMethod = nodeIds.getStartMethod() != null;

        return hasClient && hasEndpoint && hasActorNode && hasCapNode && hasStateVar && hasInitMethod && hasStartMethod;
    }
}
