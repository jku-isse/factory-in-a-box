package conveyor.opcua;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import fiab.conveyor.messages.ConveyorStatusUpdateEvent;
import fiab.conveyor.opcua.functionalunit.ConveyorFU;
import fiab.conveyor.statemachine.ConveyorStates;
import testutils.FUTestInfrastructure;
import fiab.functionalunit.connector.FUConnector;
import fiab.opcua.client.FiabOpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;
import testutils.PortUtils;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestConveyorFU {

    private FUTestInfrastructure infrastructure;
    private FUConnector conveyorConnector;

    @BeforeEach
    public void init() throws ExecutionException, InterruptedException {
        infrastructure = new FUTestInfrastructure(PortUtils.findNextFreePort());
        infrastructure.subscribeToIntraMachineEventBus();
        conveyorConnector = new FUConnector();
        infrastructure.initializeActor(ConveyorFU.props(
                infrastructure.getServer(),
                infrastructure.getServer().getRootNode(),
                conveyorConnector,
                infrastructure.getIntraMachineEventBus()), "ConveyorFU" + infrastructure.getAndIncrementRunCount());

        getProbe().expectMsgClass(ConveyorStatusUpdateEvent.class);  //Skip initial Stopped state
        getClient().connect().get();
    }


    @AfterEach
    public void teardown() {
        infrastructure.destroyActor();
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testStopFromIdleSuccess() throws Exception {
        resetConveyorForTest();

        getClient().callStringMethodBlocking(ConveyorOpcUaNodes.stopNodeId);
        expectConveyorState(ConveyorStates.STOPPING);
        expectConveyorState(ConveyorStates.STOPPED);

        String state = getClient().readStringVariableNode(ConveyorOpcUaNodes.stateNodeId);
        assertEquals(ConveyorStates.STOPPED.toString(), state);

    }

    @Test
    public void testLoadUnloadCycleSuccess() throws Exception {
        resetConveyorForTest();
        //Now we load the conveyor
        getClient().callStringMethodBlocking(ConveyorOpcUaNodes.loadNodeId);
        expectConveyorState(ConveyorStates.LOADING);
        expectConveyorState(ConveyorStates.IDLE_FULL);
        String state = getClient().readStringVariableNode(ConveyorOpcUaNodes.stateNodeId);
        assertEquals(ConveyorStates.IDLE_FULL.toString(), state);
        //Now we unload the conveyor
        getClient().callStringMethodBlocking(ConveyorOpcUaNodes.unloadNodeId);
        expectConveyorState(ConveyorStates.UNLOADING);
        expectConveyorState(ConveyorStates.IDLE_EMPTY);
        state = getClient().readStringVariableNode(ConveyorOpcUaNodes.stateNodeId);
        assertEquals(ConveyorStates.IDLE_EMPTY.toString(), state);
    }

    @Test
    public void testResetSuccess() throws Exception {
        String state = getClient().readStringVariableNode(ConveyorOpcUaNodes.stateNodeId);
        assertEquals(ConveyorStates.STOPPED.toString(), state);

        resetConveyorForTest();
    }

    private void resetConveyorForTest() throws Exception {
        //We need to reset first
        getClient().callStringMethodBlocking(ConveyorOpcUaNodes.resetNodeId);
        expectConveyorState(ConveyorStates.RESETTING);
        expectConveyorState(ConveyorStates.IDLE_EMPTY);
        String state = getClient().readStringVariableNode(ConveyorOpcUaNodes.stateNodeId);
        assertEquals(ConveyorStates.IDLE_EMPTY.toString(), state);
    }

    private void expectConveyorState(ConveyorStates conveyorState) {
        ConveyorStatusUpdateEvent machineStatusUpdateEvent = getProbe().expectMsgClass(Duration.ofSeconds(10), ConveyorStatusUpdateEvent.class);
        assertEquals(machineStatusUpdateEvent.getStatus(), conveyorState);
    }

    private TestKit getProbe() {
        return infrastructure.getProbe();
    }

    private ActorRef actorRef() {
        return infrastructure.getActorRef();
    }

    private FiabOpcUaClient getClient() {
        return infrastructure.getClient();
    }

    static class ConveyorOpcUaNodes {
        static final NodeId resetNodeId = NodeId.parse("ns=2;s=TestDevice/ConveyorFU/RESET");
        static final NodeId stopNodeId = NodeId.parse("ns=2;s=TestDevice/ConveyorFU/STOP");
        static final NodeId stateNodeId = NodeId.parse("ns=2;s=TestDevice/ConveyorFU/STATE");
        static final NodeId loadNodeId = NodeId.parse("ns=2;s=TestDevice/ConveyorFU/LOAD");
        static final NodeId unloadNodeId = NodeId.parse("ns=2;s=TestDevice/ConveyorFU/UNLOAD");
    }
}
