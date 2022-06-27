package opcua;

import fiab.opcua.server.OPCUABase;
import opcua.method.Greet;
import opcua.method.Increment;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;

public class ExampleServer {

    private static final int PORT = 4840;
    private static final String SERVER_NAME = "TestMachine";

    public static void main(String[] args) {
        OPCUABase opcuaBase = OPCUABase.createAndStartLocalServer(PORT, SERVER_NAME);

        UaFolderNode testFolder = opcuaBase.generateFolder(opcuaBase.getRootNode(), "TestFolder");
        UaVariableNode variableNode = opcuaBase.generateStringVariableNode(testFolder, "TestVariable", "Success");
        //UaMethodNode methodNode = opcuaBase.createPartialMethodNode(path, "TEST_REQUEST", "Test Method");
        UaVariableNode counterNode = opcuaBase.generateStringVariableNode(testFolder, "Counter", 0);
        //TODO make methods easier to add. There is too much information necessary to implement them
        UaMethodNode incrementNode = opcuaBase.createPartialMethodNode(testFolder, "Increment", "Counter++");
        opcuaBase.addMethodNode(testFolder, incrementNode, new Increment(incrementNode, counterNode));

        UaMethodNode greeterNode = opcuaBase.createPartialMethodNode(testFolder,"GreetMe", "Say hello");
        opcuaBase.addMethodNode(testFolder, greeterNode, new Greet(greeterNode));
    }
}
