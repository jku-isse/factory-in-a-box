package fiab.capabilityTool.opcua;

import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.capabilityTool.opcua.msg.ClientReadyNotification;
import fiab.capabilityTool.opcua.msg.WriteRequest;
import fiab.opcua.client.OPCUAClientFactory;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CapabilityManagerClient extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private OpcUaClient client;
    private NodeId plotFuNode;
    private NodeId setCapabilityMethodNodeId;

    public static Props props(String endpointURL) {
        return Props.create(CapabilityManagerClient.class, endpointURL);
    }

    public CapabilityManagerClient(String endpointURL) {
        this.client = null;
        try {
            this.client = new OPCUAClientFactory().createClient(endpointURL);
            this.client.connect().get();
            browseServerNodesRecursively(client.getAddressSpace().browse(Identifiers.RootFolder), client.getNamespaceTable());
            if (setCapabilityMethodNodeId == null || plotFuNode == null) {
                log.info("No matching method found, terminating actor for " + endpointURL);
                getSelf().tell(PoisonPill.getInstance(), self());
            } else {
                context().parent().tell(new ClientReadyNotification(endpointURL), getSelf());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(WriteRequest.class, request -> {
                    writeValue(request.getData());
                })
                .build();
    }

    private void browseServerNodesRecursively(List<ReferenceDescription> nodes, NamespaceTable namespaceTable) throws Exception {
        for (ReferenceDescription node : nodes) {
            browseServerNodesRecursively(client.getAddressSpace().browse(node.getNodeId().toNodeIdOrThrow(namespaceTable)), namespaceTable);
            if (Objects.requireNonNull(node.getBrowseName().getName()).contains("SET_PLOT_CAPABILITY")) {
                if (node.getNodeClass().equals(NodeClass.Method)) {
                    log.info("Found set capability method node: " + node.getNodeId());
                    setCapabilityMethodNodeId = node.getNodeId().toNodeIdOrThrow(namespaceTable);
                }
            } else if (Objects.requireNonNull(node.getBrowseName().getName()).contains("PLOTTER_FU")) {
                if (node.getNodeClass().equals(NodeClass.Object)) {
                    log.info("Found plotFU node: " + node.getNodeId());
                    plotFuNode = node.getNodeId().toNodeIdOrThrow(namespaceTable);
                }
            }
        }
    }

    public void writeValue(String value) {
        callMethod(setCapabilityMethodNodeId, new Variant[]{new Variant(value)}).whenCompleteAsync((s, t) -> {
            if (t == null) {
                log.info("Changed capability to " + s);
            } else {
                log.info("Could not change capability, " + t);
            }
        });
    }

    protected CompletableFuture<String> callMethod(NodeId methodId, Variant[] inputArgs) {
        CallMethodRequest request = new CallMethodRequest(plotFuNode, methodId, inputArgs);

        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();
            if (statusCode.isGood()) {
                String value = (String) (result.getOutputArguments())[0].getValue();
                return CompletableFuture.completedFuture(value);
            } else {
                StatusCode[] inputArgumentResults = result.getInputArgumentResults();
                if (inputArgumentResults != null) {
                    for (int i = 0; i < Objects.requireNonNull(inputArgumentResults).length; i++) {
                        log.error("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
                    }
                }
                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }
}
