import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.fu.HandshakeFU;
import fiab.handshake.fu.client.ClientSideHandshakeFU;
import fiab.opcua.CapabilityImplInfo;
import fiab.opcua.server.OPCUABase;
import opcua.PublicNonEncryptionTestBaseOpcUaServer;
import opcua.TestOPCUABase;
import org.bouncycastle.util.test.Test;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;

import java.time.Duration;

public class HandshakeClientTest {

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

            HandshakeFU westFU = new ClientSideHandshakeFU(opcuaBase, rootNode, "Test", self(), getContext(),
                    TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, false, true);
            WiringInfo wiringInfo = new WiringInfo();
            wiringInfo.setLocalCapabilityId("West");
            wiringInfo.setRemoteCapabilityId("DefaultHandshakeServerSide");
            wiringInfo.setRemoteEndpointURL("opc.tcp://127.0.0.1:4840");
            wiringInfo.setRemoteNodeId("ns=2;i=9");
            wiringInfo.setRemoteRole("Provided");
            try {
                new Thread(opcuaBase).start();
                westFU.provideWiringInfo(wiringInfo);
                System.out.println("Added WiringInfo");
                westFU.getFUActor().tell(HandshakeCapability.ClientMessageTypes.Reset, ActorRef.noSender());
                context().system().scheduler().scheduleOnce(Duration.ofSeconds(5), () -> {
                    westFU.getFUActor().tell(HandshakeCapability.ClientMessageTypes.Start, ActorRef.noSender());
                }, context().dispatcher());
                context().system().scheduler().scheduleOnce(Duration.ofSeconds(10), () -> {
                    westFU.getFUActor().tell(HandshakeCapability.ClientMessageTypes.Complete, ActorRef.noSender());
                }, context().dispatcher());
            } catch (Exception e) {
                e.printStackTrace();
            }
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
