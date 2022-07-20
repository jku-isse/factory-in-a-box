package plotting.opcua;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.server.OPCUABase;
import fiab.plotter.plotting.message.PlotImageRequest;
import fiab.plotter.plotting.message.PlottingStatusUpdateEvent;
import fiab.plotter.plotting.opcua.functionalunit.PlotterFU;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutils.FUTestInfrastructure;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPlottingFU {

    //Playground
    public static void main(String[] args) {
        FUTestInfrastructure infrastructure = new FUTestInfrastructure(4840);
        infrastructure.initializeActor(PlotterFU.props(infrastructure.getServer(), infrastructure.getServer().getRootNode(),
                WellknownPlotterCapability.SupportedColors.BLACK, new FUConnector(), new IntraMachineEventBus()), "Plotter");
    }

    private FUTestInfrastructure infrastructure;

    @BeforeEach
    public void setup() {
        infrastructure = new FUTestInfrastructure(4840);
        infrastructure.subscribeToIntraMachineEventBus();
        infrastructure.initializeActor(PlotterFU.props(infrastructure.getServer(), infrastructure.getServer().getRootNode(),
                WellknownPlotterCapability.SupportedColors.BLACK,
                new FUConnector(), infrastructure.getIntraMachineEventBus()), "TestPlotter");
        infrastructure.connectClient();
        expectPlottingState(BasicMachineStates.STOPPED);
    }

    @AfterEach
    public void teardown() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testFullPlottingResetAndStop() {
        new TestKit(infrastructure.getSystem()) {
            {
                assertDoesNotThrow(() -> {
                    client().callStringMethodBlocking(PlottingOpcUaNodes.resetNode);
                    expectPlottingState(BasicMachineStates.RESETTING);
                    expectPlottingState(BasicMachineStates.IDLE);

                    client().callStringMethodBlocking(PlottingOpcUaNodes.stopNode);
                    expectPlottingState(BasicMachineStates.STOPPING);
                    expectPlottingState(BasicMachineStates.STOPPED);
                });
            }
        };
    }

    @Test
    public void testPlotterFullCycle() {
        new TestKit(infrastructure.getSystem()) {
            {
                assertDoesNotThrow(() -> {
                    client().callStringMethodBlocking(PlottingOpcUaNodes.resetNode);
                    expectPlottingState(BasicMachineStates.RESETTING);
                    expectPlottingState(BasicMachineStates.IDLE);

                    client().callStringMethodBlocking(PlottingOpcUaNodes.plotNode, new Variant("TestImg"));
                    expectPlottingState(BasicMachineStates.STARTING);
                    expectPlottingState(BasicMachineStates.EXECUTE);
                    expectPlottingState(BasicMachineStates.COMPLETING);
                    expectPlottingState(BasicMachineStates.COMPLETE);

                    client().callStringMethodBlocking(PlottingOpcUaNodes.resetNode);
                    expectPlottingState(BasicMachineStates.RESETTING);
                    expectPlottingState(BasicMachineStates.IDLE);
                });
            }
        };
    }

    private FiabOpcUaClient client() {
        return infrastructure.getClient();
    }

    private void expectPlottingState(BasicMachineStates state) {
        PlottingStatusUpdateEvent event;
        event = infrastructure.getProbe().expectMsgClass(Duration.ofSeconds(10), PlottingStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }

    static class PlottingOpcUaNodes {
        static final NodeId resetNode = NodeId.parse("ns=2;s=TestDevice/PlotterFU/RESET");
        static final NodeId stopNode = NodeId.parse("ns=2;s=TestDevice/PlotterFU/STOP");
        static final NodeId plotNode = NodeId.parse("ns=2;s=TestDevice/PlotterFU/PLOT");
    }
}
