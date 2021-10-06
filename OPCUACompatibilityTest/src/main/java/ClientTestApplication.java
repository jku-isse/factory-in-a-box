import opcua.OPCUAClientFactory;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class ClientTestApplication {

    public static void main(String[] args) {
        OpcUaClient client = createClient("opc.tcp://192.168.0.40:4842");
        //OpcUaClient client = createClient("opc.tcp://192.168.0.20:4842/milo");
        if (client == null) {
            System.exit(1);
        }
        NodeId nodeId = NodeId.parse("ns=2;i=1");
        //NodeId nodeId = NodeId.parse("ns=2;s=Turntable1");
        System.out.println(getNodeById(client, nodeId));
    }

    private static UaNode getNodeById(OpcUaClient client, NodeId nodeId){
        try{
            client.connect().get();
            for(ReferenceDescription node : client.getAddressSpace().browse(Identifiers.ObjectsFolder)){
                System.out.println(node.getNodeId());
            }
            return client.getAddressSpace().getNode(nodeId);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void runMethodTest(OpcUaClient client){
        try {
            client.connect().get();
            List<ReferenceDescription> nodes = client.getAddressSpace().browse(NodeId.parse("ns=2;i=1"));
            CallMethodRequest request = new CallMethodRequest(NodeId.parse("ns=2;i=1"), NodeId.parse("ns=2;i=4"), new Variant[]{});
            CallMethodResult result = client.call(request).get();
            System.out.println(Objects.requireNonNull(result.getOutputArguments())[0].getValue());
            //client.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static OpcUaClient createClient(String endpoint) {
        try {
            return new OPCUAClientFactory().createClient(endpoint);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
