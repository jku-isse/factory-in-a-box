package miloBasics.at.jku.isse.opc.milo;

import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

public class MethodCallerJava implements Client {
	private boolean running = true;
	 public static void main(String[] args) throws Exception {
	        MethodCallerJava example = new MethodCallerJava();

	        new MyClientRunner(example).run();
	    }
	    private final Logger logger = LoggerFactory.getLogger(getClass());
    // set the url this client will connect to
    @Override
    public String getEndpointUrl() {
    	return "opc.tcp://127.0.0.1:12686/javaMotorController";
    }
    @Override
	public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				running = false;
			}
		});
		try {
			client.connect().get();
			CompletableFuture<String> helloResult = helloWorld(client, "FunnyWorld!");
			String result = helloResult.get();
			System.out.println("Result: " + result);
			
			future.complete(client);
		} catch(Exception e) {
			System.out.println("Exception");
			System.out.println(e.getMessage());
			future.complete(client);
		}
	}
    
    private CompletableFuture<String> helloWorld(OpcUaClient client, String input) {
        NodeId objectId = new NodeId(2, "HelloWorld");
        NodeId methodId = new NodeId(2, "helloworld");

        CallMethodRequest request = new CallMethodRequest(
            objectId, methodId, new Variant[]{new Variant(input)});
        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                String value = (String) l(result.getOutputArguments()).get(0).getValue();
                return CompletableFuture.completedFuture(value);
            } else {
                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }
}
