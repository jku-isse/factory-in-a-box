package opcua;

import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import opcua.method.Greet;
import opcua.method.Increment;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class TestServerAndClient {

    private static final int PORT = 4840;   //Please make sure this port is free
    private static final String SERVER_NAME = "TestMachine";
    private static final String ENDPOINT_URL = "opc.tcp://127.0.0.1:4840";
    private static final String SUCCESS = "Success";

    private static OPCUABase opcuaBase;
    private static FiabOpcUaClient opcUaClient;

    private static UaFolderNode testFolder;
    private static UaVariableNode variableNode;
    private static NodeId variableNodeId;
    private static UaVariableNode counterNode;

    private static UaMethodNode incrementNode;
    private static UaMethodNode greeterNode;

    @BeforeAll
    public static void setup() {
        try {
            opcuaBase = OPCUABase.createAndStartLocalServer(PORT, SERVER_NAME);
            opcUaClient = OPCUAClientFactory.createFIABClient(ENDPOINT_URL);

            testFolder = opcuaBase.generateFolder(opcuaBase.getRootNode(), "TestFolder");

            variableNode = opcuaBase.generateStringVariableNode(testFolder, "TestVariable", SUCCESS);
            variableNodeId = NodeId.parse("ns=2;s=TestMachine/TestFolder/TestVariable");
            counterNode = opcuaBase.generateStringVariableNode(testFolder, "Counter", 0);

            incrementNode = opcuaBase.createPartialMethodNode(testFolder, "Increment", "Counter++");
            opcuaBase.addMethodNode(testFolder, incrementNode, new Increment(incrementNode, counterNode));

            greeterNode = opcuaBase.createPartialMethodNode(testFolder, "GreetMe", "Say hello");
            opcuaBase.addMethodNode(testFolder, greeterNode, new Greet(greeterNode));
            opcUaClient.connect().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void teardown() {
        opcuaBase.shutdown();
    }

    @Test
    public void testClientGetNodeFromIdSuccess() {
        assertDoesNotThrow(() -> {
            UaNode node = opcUaClient.getNodeForId(variableNodeId);
            assertEquals(variableNode.getNodeId(), node.getNodeId());
        });
    }

    @Test
    public void testClientGetNodeFromIdFail() {
        assertThrows(UaException.class, () -> opcUaClient.getNodeForId(NodeId.parse("ns=0;s=MadeUpNodeId")));
    }

    @Test
    public void testClientGetParentSuccess() {
        assertDoesNotThrow(() -> {
            NodeId parent = opcUaClient.getParentNodeId(variableNode.getNodeId());
            assertEquals(testFolder.getNodeId(), parent);
        });
    }

    @Test
    public void testClientGetParentWithOnlyNodeIdSuccess() {
        assertDoesNotThrow(() -> {
            NodeId parent = opcUaClient.getParentNodeId(variableNodeId);
            assertEquals(testFolder.getNodeId(), parent);
        });
    }

    @Test
    public void testClientReadVariableSuccess() {
        assertDoesNotThrow(() -> {
            String result = opcUaClient.readStringVariableNode(variableNode.getNodeId());
            assertEquals(SUCCESS, result);
        });
    }

    @Test
    public void testClientMethodCallSuccess() {
        assertDoesNotThrow(() -> {
            int previous = Integer.parseInt(opcUaClient.readStringVariableNode(counterNode.getNodeId()));
            String result = opcUaClient.callStringMethodBlocking(incrementNode.getNodeId());
            assertEquals("", result);
            result = opcUaClient.readStringVariableNode(counterNode.getNodeId());
            assertEquals(previous + 1, Integer.parseInt(result));
        });
    }

    @Test
    public void testClientMethodCallWithParameterSuccess() {
        assertDoesNotThrow(() -> {
            String result = opcUaClient.callStringMethodBlocking(greeterNode.getNodeId(), new Variant("Tester, that's you"));
            assertEquals("Hello Tester, that's you!", result);
        });
    }

    @Test
    public void testClientMethodCallAsyncSuccess() {
        assertDoesNotThrow(() -> {
            String result = opcUaClient.callStringMethod(greeterNode.getNodeId(), new Variant("Tester, that's you")).get();
            assertEquals("Hello Tester, that's you!", result);

            result = opcUaClient.callStringMethod(greeterNode.getNodeId(), new Variant[]{
                    new Variant("from Opcua")
            }).get();
            assertEquals("Hello from Opcua!", result);
        });
    }


    @Test
    public void testClientMethodCallAsyncSuccessWithoutGet() {
        assertDoesNotThrow(() -> {
            opcUaClient.callStringMethod(greeterNode.getNodeId(), new Variant("Tester, that's you"))
                    .thenAccept(result -> assertEquals("Hello Tester, that's you!", result));

            opcUaClient.callStringMethod(greeterNode.getNodeId(), new Variant[]{new Variant("from Opcua")})
                    .thenAccept(result -> assertEquals("Hello from Opcua!", result));

            opcUaClient.callStringMethod(incrementNode.getNodeId(), new Variant(new Variant[]{}))
                    .thenAccept(result -> assertEquals("", result));
        });
    }

    @Test
    public void testClientGetChildNodeSuccessful() {
        assertDoesNotThrow(() -> {
            assertNotNull(opcUaClient
                    .getChildNodeByBrowseName(testFolder.getNodeId(), greeterNode.getBrowseName().getName()));
        });
    }

    //TODO test subscriptions
}
