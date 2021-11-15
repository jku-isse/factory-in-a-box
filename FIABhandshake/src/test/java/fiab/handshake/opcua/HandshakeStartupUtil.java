package fiab.handshake.opcua;

import akka.actor.ActorSystem;
import fiab.handshake.opcua.actor.StandaloneHandshakeActor;
import fiab.opcua.server.OPCUABase;
import fiab.opcua.server.PublicNonEncryptionBaseOpcUaServer;

public class HandshakeStartupUtil {

    private static final String NAMESPACE_URI = "urn:factory-in-a-box";

    public static void startServerHandshakeApplication() throws Exception {
        ActorSystem system = ActorSystem.create();
        PublicNonEncryptionBaseOpcUaServer server = new PublicNonEncryptionBaseOpcUaServer(0, "ServerHS");
        OPCUABase opcuaBase = new OPCUABase(server.getServer(), NAMESPACE_URI, "ServerHandshakeMachine");
        system.actorOf(StandaloneHandshakeActor.props(opcuaBase, "Handshake", true));
        new Thread(opcuaBase).start();

        //system.actorOf(StandaloneHandshakeActor.props())
    }

    public static void startClientHandshakeApplication() throws Exception {
        ActorSystem system = ActorSystem.create();
        PublicNonEncryptionBaseOpcUaServer server = new PublicNonEncryptionBaseOpcUaServer(1, "ClientHS");
        OPCUABase opcuaBase =  new OPCUABase(server.getServer(), NAMESPACE_URI, "ClientHandshakeMachine");
        system.actorOf(StandaloneHandshakeActor.props(opcuaBase, "Handshake", false));
    }

}
