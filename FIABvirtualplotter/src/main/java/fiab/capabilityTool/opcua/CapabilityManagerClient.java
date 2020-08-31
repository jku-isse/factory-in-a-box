package fiab.capabilityTool.opcua;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.google.common.collect.ImmutableList;
import fiab.capabilityTool.gui.msg.ClientReadyNotification;
import fiab.capabilityTool.gui.msg.ReadNotification;
import fiab.capabilityTool.gui.msg.ReadRequest;
import fiab.capabilityTool.gui.msg.WriteRequest;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CapabilityManagerClient extends AbstractActor {

    private OpcUaClient client;

    public static Props props(String endpointURL) {
        return Props.create(CapabilityManagerClient.class, endpointURL);
    }

    public CapabilityManagerClient(String endpointURL) {
        client = null;
        try {
            client = new OPCUAClientFactory().createClient(endpointURL);
            client.connect().get();
            context().parent().tell(new ClientReadyNotification(), getSelf());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ReadRequest.class, request -> {
                    sender().tell(new ReadNotification(readValue()), self());
                }).match(WriteRequest.class, request -> {
                    writeValue(request.getData());
                })
                .build();
    }

    public void writeValue(String value) {
        //plotcapability black: http://factory-in-a-box.fiab/capabilities/plot/color/BLACK
        NodeId methodId = new NodeId(2, "Plotter/PLOTTER_FU/SET_PLOT_CAPABILITY");//NodeId.parse("ns=1;s=Plotter/PLOTTER_FU/SET_CAPABILITIES");
        callMethod(methodId, new Variant[]{new Variant(value)}).whenCompleteAsync((s, t) -> System.out.println("S: " + s + ", T: " + t));
    }

    public String readValue() {
        if (client != null) {
            try {
                List<NodeId> nodeIdList = ImmutableList.of(new NodeId(2, "TestPlotter/Plotting_FU/CAPABILITIES/CAPABILITY/TYPE"));
                CompletableFuture<DataValue> dataValues = client.readValue(1000.0d, TimestampsToReturn.Both, nodeIdList.get(0));
                return dataValues.get(1000L, TimeUnit.MILLISECONDS).getValue().getValue().toString();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
                return "Not implemented";
            }
        }
        return "Client is null";
    }

    protected CompletableFuture<String> callMethod(NodeId methodId, Variant[] inputArgs) {

        CallMethodRequest request = new CallMethodRequest(
                new NodeId(1, 324), methodId, inputArgs);

        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                String value = (String) (result.getOutputArguments())[0].getValue();
                return CompletableFuture.completedFuture(value);
            } else {
                StatusCode[] inputArgumentResults = result.getInputArgumentResults();
                for (int i = 0; i < inputArgumentResults.length; i++) {
                    System.out.printf("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
                }

                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }

}
