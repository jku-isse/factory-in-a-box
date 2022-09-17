package turning;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.opcua.server.OPCUABase;
import fiab.opcua.server.PublicNonEncryptionBaseOpcUaServer;
import testutils.FUTestInfrastructure;
import fiab.functionalunit.connector.FUConnector;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.turntable.turning.messages.TurningStatusUpdateEvent;
import fiab.turntable.turning.opcua.functionalunit.TurningFU;
import fiab.turntable.turning.statemachine.TurningStates;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.*;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestTurningFU {

    private static FUTestInfrastructure infrastructure;
    private static FUConnector turningConnector;

    //Playground
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        OPCUABase base = OPCUABase.createAndStartLocalServer(4840, "TestDevice");
        system.actorOf(TurningFU.props(base, base.getRootNode(), new FUConnector(), new IntraMachineEventBus()));
    }

    @BeforeAll
    public static void setup() {
        infrastructure = new FUTestInfrastructure(4851);
        infrastructure.subscribeToIntraMachineEventBus();
        turningConnector = new FUConnector();
    }

    @BeforeEach
    public void init() throws ExecutionException, InterruptedException {
        infrastructure.initializeActor(TurningFU.props(
                infrastructure.getServer(),
                infrastructure.getServer().getRootNode(),
                turningConnector,
                infrastructure.getIntraMachineEventBus()), "TurningFU"+infrastructure.getAndIncrementRunCount());

        getProbe().expectMsgClass(TurningStatusUpdateEvent.class);  //Skip initial Stopped state
        client().connect().get();
    }

    @AfterEach
    public void teardown() {
        infrastructure.destroyActor();
    }

    @AfterAll
    public static void cleanup() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testResetSuccess() throws Exception {
        String state = client().readStringVariableNode(TurningOpcUaNodes.stateNodeId);
        assertEquals(TurningStates.STOPPED.toString(), state);

        resetTurningForTest();
    }

    @Test
    public void testStopFromIdleSuccess() throws Exception {
        resetTurningForTest();

        client().callStringMethodBlocking(TurningOpcUaNodes.stopNodeId);
        expectTurningState(TurningStates.STOPPING);
        expectTurningState(TurningStates.STOPPED);

        String state = client().readStringVariableNode(TurningOpcUaNodes.stateNodeId);
        assertEquals(TurningStates.STOPPED.toString(), state);

    }

    @Test
    public void testTurnToNorthSuccess() throws Exception {
        resetTurningForTest();
        //Now we load the conveyor
        client().callStringMethodBlocking(TurningOpcUaNodes.turnToNodeId, new Variant("NORTH"));
        expectTurningState(TurningStates.STARTING);
        expectTurningState(TurningStates.EXECUTING);
        expectTurningState(TurningStates.COMPLETING);
        expectTurningState(TurningStates.COMPLETE);
        String state = client().readStringVariableNode(TurningOpcUaNodes.stateNodeId);
        assertEquals(TurningStates.COMPLETE.toString(), state);
    }

    @Test
    public void testTurnToEastSuccess() throws Exception {
        resetTurningForTest();
        //Now we load the conveyor
        client().callStringMethodBlocking(TurningOpcUaNodes.turnToNodeId, new Variant("EAST"));
        expectTurningState(TurningStates.STARTING);
        expectTurningState(TurningStates.EXECUTING);
        expectTurningState(TurningStates.COMPLETING);
        expectTurningState(TurningStates.COMPLETE);
        String state = client().readStringVariableNode(TurningOpcUaNodes.stateNodeId);
        assertEquals(TurningStates.COMPLETE.toString(), state);
    }

    @Test
    public void testTurnToSouthSuccess() throws Exception {
        resetTurningForTest();
        //Now we load the conveyor
        client().callStringMethodBlocking(TurningOpcUaNodes.turnToNodeId, new Variant("SOUTH"));
        expectTurningState(TurningStates.STARTING);
        expectTurningState(TurningStates.EXECUTING);
        expectTurningState(TurningStates.COMPLETING);
        expectTurningState(TurningStates.COMPLETE);
        String state = client().readStringVariableNode(TurningOpcUaNodes.stateNodeId);
        assertEquals(TurningStates.COMPLETE.toString(), state);
    }

    @Test
    public void testTurnToWestSuccess() throws Exception {
        resetTurningForTest();
        //Now we load the conveyor
        client().callStringMethodBlocking(TurningOpcUaNodes.turnToNodeId, new Variant("WEST"));
        expectTurningState(TurningStates.STARTING);
        expectTurningState(TurningStates.EXECUTING);
        expectTurningState(TurningStates.COMPLETING);
        expectTurningState(TurningStates.COMPLETE);
        String state = client().readStringVariableNode(TurningOpcUaNodes.stateNodeId);
        assertEquals(TurningStates.COMPLETE.toString(), state);
    }

    private TestKit getProbe() {
        return infrastructure.getProbe();
    }

    private ActorRef actorRef() {
        return infrastructure.getActorRef();
    }

    private FiabOpcUaClient client() {
        return infrastructure.getClient();
    }

    private void resetTurningForTest() throws Exception {
        //We need to reset first
        client().callStringMethodBlocking(TurningOpcUaNodes.resetNodeId);
        expectTurningState(TurningStates.RESETTING);
        expectTurningState(TurningStates.IDLE);
        String state = client().readStringVariableNode(TurningOpcUaNodes.stateNodeId);
        assertEquals(TurningStates.IDLE.toString(), state);
    }

    private void expectTurningState(TurningStates turningStates) {
        TurningStatusUpdateEvent machineStatusUpdateEvent = getProbe().expectMsgClass(TurningStatusUpdateEvent.class);
        assertEquals(machineStatusUpdateEvent.getStatus(), turningStates);
    }

    static class TurningOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=TestDevice/TurningFU/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=TestDevice/TurningFU/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=TestDevice/TurningFU/STATE");
        static final NodeId turnToNodeId = NodeId.parse("ns=2;s=TestDevice/TurningFU/TURN_TO");
    }

}
