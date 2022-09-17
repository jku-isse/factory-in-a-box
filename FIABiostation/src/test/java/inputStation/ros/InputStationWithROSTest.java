package inputStation.ros;

import akka.testkit.javadsl.TestKit;
import client.ClientNode;
import client.ROSClient;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.iostation.opcua.OpcUaInputStationActor;
import fiab.iostation.opcua.OpcUaInputStationActorROS;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.server.OPCUABase;
import internal.FIABNodeConfig;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;
import org.ros.exception.ServiceNotFoundException;
import org.ros.node.NodeConfiguration;
import ros_basic_machine_msg.ResetService;
import ros_io_msg.EjectService;
import testutils.FUTestInfrastructure;
import testutils.PortUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("SystemTest")
public class InputStationWithROSTest {

    public static final String ROS_MASTER_IP_RPI = "192.168.133.122";
    public static final String ROS_MASTER_IP_LOCAL = "192.168.133.88";
    private static FUTestInfrastructure infrastructure;

    //Playground
    public static void main(String[] args) {
        String rosMasterIp = ROS_MASTER_IP_LOCAL;
        FUTestInfrastructure infrastructure = new FUTestInfrastructure(4840);
        FUConnector requestConnector = new FUConnector();
        infrastructure.subscribeToIntraMachineEventBus();
        OPCUABase opcuaBase = infrastructure.getServer();
        try {
            NodeConfiguration nodeConfiguration = FIABNodeConfig.createNodeConfiguration("127.0.0.1",
                    "TestNodeId", new URI("http://" + rosMasterIp + ":11311"));
            ROSClient rosClient = ROSClient.newInstance(ClientNode.class, nodeConfiguration);
            //Call this for each messsage type you want to support
            //e.g.  rosClient.createServiceClient("TurnToPos", TurnToPos._TYPE);
            rosClient.createServiceClient("FIAB_reset_service", ResetService._TYPE);
            rosClient.createServiceClient("FIAB_eject_service", EjectService._TYPE);
            infrastructure.initializeActor(OpcUaInputStationActorROS.props(rosClient, opcuaBase, opcuaBase.getRootNode(), new MachineEventBus()),
                    "InputStation" + infrastructure.getAndIncrementRunCount());
        } catch (URISyntaxException | ServiceNotFoundException e) {
            e.printStackTrace();
        }
    }

    @BeforeEach
    public void setup() {
        int opcuaServerPort = PortUtils.findNextFreePort();
        infrastructure = new FUTestInfrastructure(opcuaServerPort);
        String rosMasterIp = ROS_MASTER_IP_LOCAL;
        infrastructure.subscribeToMachineEventBus();
        OPCUABase opcuaBase = infrastructure.getServer();
        try {
            NodeConfiguration nodeConfiguration = FIABNodeConfig.createNodeConfiguration("127.0.0.1",
                    "TestNodeId", new URI("http://" + rosMasterIp + ":11311"));
            ROSClient rosClient = ROSClient.newInstance(ClientNode.class, nodeConfiguration);
            //Call this for each messsage type you want to support
            //e.g.  rosClient.createServiceClient("TurnToPos", TurnToPos._TYPE);
            rosClient.createServiceClient("FIAB_reset_service", ResetService._TYPE);
            rosClient.createServiceClient("FIAB_eject_service", EjectService._TYPE);
            infrastructure.initializeActor(OpcUaInputStationActorROS.props(rosClient, opcuaBase, opcuaBase.getRootNode(), infrastructure.getMachineEventBus()),
                    "InputStation" + opcuaServerPort);
        } catch (URISyntaxException | ServiceNotFoundException e) {
            e.printStackTrace();
        }
        infrastructure.connectClient();
    }

    @AfterEach
    public void teardown() {
        infrastructure.getIntraMachineEventBus().unsubscribe(infrastructure.getProbe().getRef());
    }

    @AfterAll
    public static void cleanup() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testResetInputStation(){
        assertDoesNotThrow(() -> {
            client().callStringMethodBlocking(InputStationOpcUaNodes.resetNodeId);
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(ServerSideStates.IDLE_LOADED);
        });
    }

    @Test
    public void testInputStation() {
        assertDoesNotThrow(() -> {
            client().callStringMethodBlocking(InputStationOpcUaNodes.resetNodeId);
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(ServerSideStates.IDLE_LOADED);

            client().callStringMethodBlocking(InputStationOpcUaNodes.initNodeId);
            expectServerSideState(ServerSideStates.STARTING);
            expectServerSideState(ServerSideStates.PREPARING);
            expectServerSideState(ServerSideStates.READY_LOADED);

            client().callStringMethodBlocking(InputStationOpcUaNodes.startNodeId);
            expectServerSideState(ServerSideStates.EXECUTE);
            expectServerSideState(ServerSideStates.COMPLETING);
            expectServerSideState(ServerSideStates.COMPLETE);
            String state = client().readStringVariableNode(InputStationOpcUaNodes.stateNodeId);
            assertEquals(state, ServerSideStates.COMPLETE.name());

            client().callStringMethodBlocking(InputStationOpcUaNodes.resetNodeId);
            expectServerSideState(ServerSideStates.RESETTING);
            expectServerSideState(ServerSideStates.IDLE_LOADED);
        });
    }

    private void expectServerSideState(ServerSideStates serverSideState) {
        ServerHandshakeStatusUpdateEvent machineStatusUpdateEvent;
        machineStatusUpdateEvent = probe().expectMsgClass(Duration.ofSeconds(10), ServerHandshakeStatusUpdateEvent.class);
        assertEquals(serverSideState, machineStatusUpdateEvent.getStatus());
    }

    private FiabOpcUaClient client() {
        return infrastructure.getClient();
    }

    private TestKit probe() {
        return infrastructure.getProbe();
    }

    static class InputStationOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/STATE");
        static final NodeId initNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/INIT_HANDOVER");
        static final NodeId startNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/START_HANDOVER");
        static final NodeId completeNodeId = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/COMPLETE");
    }
}
