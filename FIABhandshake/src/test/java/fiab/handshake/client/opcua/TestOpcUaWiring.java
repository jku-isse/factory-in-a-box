package fiab.handshake.client.opcua;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.FUConnector;
import fiab.handshake.client.opcua.functionalunit.ClientHandshakeFU;
import fiab.handshake.client.util.WiringInfoOpcUaUtil;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import testutils.PortUtils;

import static org.junit.jupiter.api.Assertions.*;

@Tag("IntegrationTest")
public class TestOpcUaWiring {

    private ActorSystem system;

    private FiabOpcUaClient client;
    private WiringInfo serverWiringInfo;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create();
        int clientPort = PortUtils.findNextFreePort();
        int serverPort = PortUtils.findNextFreePort();
        OPCUABase clientOpcUaBase = OPCUABase.createAndStartLocalServer(clientPort, "TestDevice");
        OPCUABase serverOpcUaBase = OPCUABase.createAndStartLocalServer(serverPort, "ServerHandshakeMachine");
        system.actorOf(ServerHandshakeFU.propsForStandaloneFU(serverOpcUaBase, serverOpcUaBase.getRootNode(), "DefaultServerSideHandshake"));
        system.actorOf(ClientHandshakeFU.propsForStandaloneFU(clientOpcUaBase, clientOpcUaBase.getRootNode(), "NORTH_CLIENT"));
        try {
            this.client = OPCUAClientFactory.createFIABClientAndConnect("opc.tcp://127.0.0.1:" + clientPort);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        serverWiringInfo = createTestWiringInfo(serverPort);
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testSetNewWiringViaOpcUa() {
        assertDoesNotThrow(() -> {
            String result = client.callStringMethodBlocking(ClientHsOpcUaNodes.setWiringNodeId,
                    WiringInfoOpcUaUtil.wiringInfoAsOpcUaArgs(serverWiringInfo));
            assertEquals("Ok", result);
            String connectedEndpoint = client.readStringVariableNode(ClientHsOpcUaNodes.remoteWiringEndpointNodeId);
            assertEquals(serverWiringInfo.getRemoteEndpointURL(), connectedEndpoint);
        });
    }

    @Test
    public void testUnsetWiringViaOpcUa() {
        assertDoesNotThrow(() -> {
            String result = client.callStringMethodBlocking(ClientHsOpcUaNodes.setWiringNodeId,
                    WiringInfoOpcUaUtil.wiringInfoAsOpcUaArgs(serverWiringInfo));
            assertEquals("Ok", result);
            String connectedEndpoint = client.readStringVariableNode(ClientHsOpcUaNodes.remoteWiringEndpointNodeId);
            assertEquals(serverWiringInfo.getRemoteEndpointURL(), connectedEndpoint);
            result = client.callStringMethodBlocking(ClientHsOpcUaNodes.setWiringNodeId,
                    WiringInfoOpcUaUtil.wiringInfoAsOpcUaArgs(new WiringInfoBuilder().build()));
            assertEquals("Ok", result);
            connectedEndpoint = client.readStringVariableNode(ClientHsOpcUaNodes.remoteWiringEndpointNodeId);
            assertEquals("", connectedEndpoint);
        });
    }

    private WiringInfo createTestWiringInfo(int port) {
        return WiringInfoBuilder.create()
                .setLocalCapabilityId("NORTH_CLIENT")
                .setRemoteCapabilityId("DefaultServerSideHandshake")
                .setRemoteRole("RemoteRole1")
                .setRemoteNodeId("ns=2;s=ServerHandshakeMachine/HANDSHAKE_FU_DefaultServerSideHandshake/CAPABILITIES/CAPABILITY")
                .setRemoteEndpointURL("opc.tcp://127.0.0.1:" + port)
                .build();
    }

    static class ClientHsOpcUaNodes {

        static final NodeId setWiringNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/SET_WIRING");

        static final NodeId remoteWiringEndpointNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/WIRING_INFO/REMOTE_ENDPOINT_URL");
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/STATE");
        static final NodeId startNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/START");
        static final NodeId completeNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU_NORTH_CLIENT/COMPLETE");
    }
}
