package server;

import fiab.opcua.server.FastPublicNonEncryptionBaseOpcUaServer;
import fiab.opcua.server.OPCUABase;

import java.time.Duration;
import java.time.Instant;

public class FastServerTest {

    public static void main(String[] args) {
        try {
            System.out.println("Creating server");
            Instant start = Instant.now();
            FastPublicNonEncryptionBaseOpcUaServer server = new FastPublicNonEncryptionBaseOpcUaServer(0, "Test");
            System.out.println("Creating opc ua base");
            Instant end = Instant.now();
            System.out.println("Created opc ua base in " + Duration.between(start, end));
            OPCUABase opcuaBase = new OPCUABase(server.getServer(), "namespaceuri", "TestMachine");
            System.out.println("Preparing opc ua base");
            opcuaBase.prepareRootNode();
            System.out.println("Starting Test Server...");
            new Thread(opcuaBase).start();
            System.out.println("Server up and running");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
