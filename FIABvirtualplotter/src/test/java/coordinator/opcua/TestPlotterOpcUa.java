package coordinator.opcua;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import fiab.conveyor.messages.LoadConveyorRequest;
import fiab.conveyor.messages.UnloadConveyorRequest;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.core.capabilities.handshake.server.TransportAreaStatusOverrideRequest;
import fiab.core.capabilities.plotting.PlotRequest;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.plotter.opcua.OpcUaPlotterCoordinatorActor;
import fiab.plotter.plotting.PlottingCapability;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutils.FUTestInfrastructure;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPlotterOpcUa {

    public static void main(String[] args) {
        FUTestInfrastructure infrastructure = new FUTestInfrastructure(4840);
        infrastructure.initializeActor(OpcUaPlotterCoordinatorActor.props(infrastructure.getServer(), infrastructure.getServer().getRootNode(),
                WellknownPlotterCapability.SupportedColors.BLACK, infrastructure.getMachineEventBus(), infrastructure.getIntraMachineEventBus()), "TestPlotter");
    }

    private FUTestInfrastructure infrastructure;
    private TestKit probe;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        infrastructure = new FUTestInfrastructure(4840);
        probe = new TestKit(infrastructure.getSystem());
        infrastructure.subscribeToIntraMachineEventBus();
        infrastructure.getMachineEventBus().subscribe(probe.getRef(), new FUSubscriptionClassifier(probe.getRef().path().name(), "*"));
        infrastructure.initializeActor(OpcUaPlotterCoordinatorActor.props(infrastructure.getServer(), infrastructure.getServer().getRootNode(),
                WellknownPlotterCapability.SupportedColors.BLACK, infrastructure.getMachineEventBus(), infrastructure.getIntraMachineEventBus()), "TestPlotter");
        client().connectFIABClient().get();
        expectMachineState(BasicMachineStates.STOPPED);
    }

    @AfterEach
    public void teardown() {
        infrastructure.disconnectClient();
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testResetAndStopPlotter() {
        assertDoesNotThrow(() -> {
            client().callStringMethodBlocking(PlotterOpcUaNodes.resetNode);
            expectMachineState(BasicMachineStates.RESETTING);
            expectMachineState(BasicMachineStates.IDLE);

            client().callStringMethodBlocking(PlotterOpcUaNodes.stopNode);
            expectMachineState(BasicMachineStates.STOPPING);
            expectMachineState(BasicMachineStates.STOPPED);
        });
    }

    @Test
    public void testPlotterFullCycle() {
        assertDoesNotThrow(() -> {
            client().callStringMethodBlocking(PlotterOpcUaNodes.resetNode);
            expectMachineState(BasicMachineStates.RESETTING);
            expectMachineState(BasicMachineStates.IDLE);

            client().callStringMethodBlocking(PlotterOpcUaNodes.plotNode, new Variant("TestImage"));
            expectMachineState(BasicMachineStates.STARTING);

            intraMachineProbe().fishForMessage(Duration.ofSeconds(5), "Wait for server handshake to reach idle",
                    msg -> msg instanceof ServerHandshakeStatusUpdateEvent
                            && ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.IDLE_EMPTY);
            client().callStringMethodBlocking(PlotterOpcUaNodes.initHandshakeNode);
            client().callStringMethodBlocking(PlotterOpcUaNodes.startHandshakeNode);

            expectMachineState(BasicMachineStates.EXECUTE);
            expectMachineState(BasicMachineStates.COMPLETING);

            intraMachineProbe().fishForMessage(Duration.ofSeconds(5), "Wait for server handshake to reach idle",
                    msg -> msg instanceof ServerHandshakeStatusUpdateEvent
                            && ((ServerHandshakeStatusUpdateEvent) msg).getStatus() == ServerSideStates.IDLE_LOADED);
            client().callStringMethodBlocking(PlotterOpcUaNodes.initHandshakeNode);
            client().callStringMethodBlocking(PlotterOpcUaNodes.startHandshakeNode);

            expectMachineState(BasicMachineStates.COMPLETE);
            client().callStringMethodBlocking(PlotterOpcUaNodes.resetNode);
            expectMachineState(BasicMachineStates.RESETTING);
            expectMachineState(BasicMachineStates.IDLE);
        });
    }

    //TODO add tests for changing supported color

    private void expectMachineState(BasicMachineStates state) {
        MachineStatusUpdateEvent event = probe.expectMsgClass(Duration.ofSeconds(15), MachineStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }

    private FiabOpcUaClient client() {
        return infrastructure.getClient();
    }

    private TestKit intraMachineProbe() {
        return infrastructure.getProbe();
    }

    static class PlotterOpcUaNodes {
        static final NodeId resetNode = NodeId.parse("ns=2;s=TestDevice/RESET");
        static final NodeId stopNode = NodeId.parse("ns=2;s=TestDevice/STOP");
        static final NodeId plotNode = NodeId.parse("ns=2;s=TestDevice/PLOT");
        static final NodeId setCapNode = NodeId.parse("ns=2;s=TestDevice/SET_CAPABILITY");

        static final NodeId initHandshakeNode = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/INIT_HANDOVER");
        static final NodeId startHandshakeNode = NodeId.parse("ns=2;s=TestDevice/HANDSHAKE_FU/START_HANDOVER");
    }
}
