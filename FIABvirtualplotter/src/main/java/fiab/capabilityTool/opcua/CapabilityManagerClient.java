package fiab.capabilityTool.opcua;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.google.common.collect.ImmutableList;
import fiab.capabilityTool.gui.msg.ClientReadyNotification;
import fiab.capabilityTool.gui.msg.ReadNotification;
import fiab.capabilityTool.gui.msg.ReadRequest;
import fiab.capabilityTool.gui.msg.WriteRequest;
import fiab.capabilityTool.opcua.methods.SetCapability;
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

    public void writeValue(String value) throws ExecutionException, InterruptedException {
        NodeId objectId = new NodeId(1, 119);//NodeId.parse("ns=1;i=119");
        NodeId methodId = new NodeId(1, "Plotter/PLOTTER_FU/SET_CAPABILITIES");//NodeId.parse("ns=1;s=Plotter/PLOTTER_FU/SET_CAPABILITIES");
        CallMethodRequest methodRequest = new CallMethodRequest(
                objectId,
                methodId,
                new Variant[]{new Variant(value)});
        callMethod(methodRequest).get();
    }

    public String readValue() {
        if (client != null) {
            try {
                //client.connect().get();
                List<NodeId> nodeIdList = ImmutableList.of(new NodeId(1, "Plotter/PLOTTER_FU/CAPABILITIES/CAPABILITY1/TYPE"));
                CompletableFuture<DataValue> dataValues = client.readValue(1000.0d, TimestampsToReturn.Both, nodeIdList.get(0));
                return dataValues.get(1000L, TimeUnit.MILLISECONDS).getValue().getValue().toString();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
                return "Not implemented";
            }
        }
        return "Client is null";
    }

    protected CompletableFuture<String> callMethod(CallMethodRequest request) {

        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                //String value = (String) (result.getOutputArguments())[0].getValue();
                return CompletableFuture.completedFuture("OK");
            } else {
                StatusCode[] inputArgumentResults = result.getInputArgumentResults();
                for (int i = 0; i < Objects.requireNonNull(inputArgumentResults).length; i++) {
                    //logger.error("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
                    System.out.printf("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
                }

                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }

}
