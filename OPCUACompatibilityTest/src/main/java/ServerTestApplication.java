import opcua.PublicNonEncryptionTestBaseOpcUaServer;
import opcua.TestOPCUABase;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

public class ServerTestApplication {

    public static void main(String[] args) {
        PublicNonEncryptionTestBaseOpcUaServer server = createServer();
        if (server == null) {
            System.exit(1);
        }
        TestOPCUABase testOpcuaBase = new TestOPCUABase(server.getServer(), "fiab", "TestMachine");
        UaFolderNode rootNode = testOpcuaBase.prepareRootNode();
        UaFolderNode methodRoot = testOpcuaBase.generateFolder(rootNode, "Test", "Test");
        UaMethodNode methodNode = testOpcuaBase.createPartialMethodNode("Test", "Method", "Example method");
        TestMethod testMethod = new TestMethod(methodNode);
        testOpcuaBase.addMethodNode(methodRoot, methodNode, testMethod);
        testOpcuaBase.run();
    }

    private static PublicNonEncryptionTestBaseOpcUaServer createServer() {
        try {
            return new PublicNonEncryptionTestBaseOpcUaServer(0, "TestServer");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class TestMethod extends AbstractMethodInvocationHandler {


        /**
         * @param node the {@link UaMethodNode} this handler will be installed on.
         */
        public TestMethod(UaMethodNode node) {
            super(node);
        }

        @Override
        public Argument[] getInputArguments() {
            return new Argument[0];
        }

        @Override
        public Argument[] getOutputArguments() {
            return new Argument[]{};
        }

        @Override
        protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
            return new Variant[]{new Variant("Hello from milo :)")};
        }
    }
}
