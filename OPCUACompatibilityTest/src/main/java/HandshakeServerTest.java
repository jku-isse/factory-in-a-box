import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.fu.HandshakeFU;
import fiab.handshake.fu.client.ClientSideHandshakeFU;
import fiab.handshake.fu.server.ServerSideHandshakeFU;
import fiab.opcua.server.OPCUABase;
import opcua.OPCUAClientFactory;
import opcua.PublicNonEncryptionTestBaseOpcUaServer;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class HandshakeServerTest {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        system.actorOf(TestActor.props());
    }


    private static class TestActor extends AbstractActor {

        public static Props props() {
            return Props.create(TestActor.class, () -> new TestActor());
        }

        private TestActor() {
            PublicNonEncryptionTestBaseOpcUaServer server = createServer();
            if (server == null) {
                System.exit(1);
            }
            OPCUABase opcuaBase = new OPCUABase(server.getServer(), "fiab", "TestMachine");
            UaFolderNode rootNode = opcuaBase.prepareRootNode();

            HandshakeFU westFU = new ServerSideHandshakeFU(opcuaBase, rootNode, "Test", self(), getContext(),
                    TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_SERVER, true, true);

            new Thread(opcuaBase).start();

        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create()
                    .matchAny(msg -> System.out.println(msg)).build();
        }

        private static PublicNonEncryptionTestBaseOpcUaServer createServer() {
            try {
                return new PublicNonEncryptionTestBaseOpcUaServer(1, "TestServer");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
