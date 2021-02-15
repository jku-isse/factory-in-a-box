package fiab.capabilityManager.opcua;

import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.capabilityManager.opcua.msg.ClientReadyNotification;
import fiab.capabilityManager.opcua.msg.WriteRequest;
import fiab.opcua.client.OPCUAClientFactory;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CapabilityManagerClient extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private OpcUaClient client;
    private NodeId setCapabilityMethodNodeId;
    private NodeId plotFuNodeId;

    public static Props props(String endpointURL) {
        return Props.create(CapabilityManagerClient.class, endpointURL);
    }

    public CapabilityManagerClient(String endpointURL) {
        this.client = null;
        try {
            this.client = new OPCUAClientFactory().createClient(endpointURL);
            this.client.connect().get();
            browseServerNodesRecursively(client.getAddressSpace().browse(Identifiers.RootFolder).get());
            if (setCapabilityMethodNodeId == null) {
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

    private void browseServerNodesRecursively(List<Node> nodes) throws ExecutionException, InterruptedException {
        for (Node node : nodes) {
            browseServerNodesRecursively(client.getAddressSpace().browse(node.getNodeId().get()).get());
            if (Objects.requireNonNull(node.getBrowseName().get().getName()).contains("SET_PLOT_CAPABILITY")) {
                if (node instanceof UaMethodNode) {
                    log.info("Found set capability method node: " + node.getNodeId().get());
                    setCapabilityMethodNodeId = node.getNodeId().get();
                }
            } else if (Objects.requireNonNull(node.getBrowseName().get().getName()).equalsIgnoreCase("PLOTTER_FU")) {
                    log.info("Found Plotter_FU node: " + node.getNodeId().get());
                    plotFuNodeId = node.getNodeId().get();
            }
        }
    }

    public void writeValue(String value) {
        callMethod(setCapabilityMethodNodeId, new Variant[]{new Variant(value)}).whenCompleteAsync((s, t) -> {
            if (t == null) {
                log.info("Changed capability to " + s);
            } else {
                log.info("Could not change capability, " + t);
                t.printStackTrace();
            }
        });
    }

    protected CompletableFuture<String> callMethod(NodeId methodId, Variant[] inputArgs) {
        CallMethodRequest request = new CallMethodRequest(plotFuNodeId, methodId, inputArgs);

        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();
            if (statusCode.isGood()) {
                String value = (String) (result.getOutputArguments())[0].getValue();
                log.info("Received result: " + value);
                return CompletableFuture.completedFuture(value);
            } else {
                StatusCode[] inputArgumentResults = result.getInputArgumentResults();
                for (int i = 0; i < inputArgumentResults.length; i++) {
                    log.error("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
                }

                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }
}
