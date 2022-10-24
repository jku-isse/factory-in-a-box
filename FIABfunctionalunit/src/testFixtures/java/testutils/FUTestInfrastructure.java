package testutils;

import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;

import java.util.concurrent.ExecutionException;

public class FUTestInfrastructure extends ActorTestInfrastructure {

    protected int port;
    protected OPCUABase opcuaBase;
    protected FiabOpcUaClient opcUaClient;

    public FUTestInfrastructure(int port) {
        super();
        this.port = port;
        opcuaBase = OPCUABase.createAndStartLocalServer(port, "TestDevice");
        try {
            opcUaClient = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:" + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public OPCUABase getServer() {
        return opcuaBase;
    }

    public FiabOpcUaClient getClient() {
        return opcUaClient;
    }

    public void connectClient() {
        try {
            opcUaClient.connect().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void disconnectClient() {
        try {
            opcUaClient.disconnect().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void shutdownServer() {
        opcuaBase.shutDownOpcUaBase();
    }

    @Override
    public void shutdownInfrastructure() {
        super.shutdownInfrastructure();
        //In addition, we need to shut down the server.
        opcuaBase.shutDownOpcUaBase();
    }
}
