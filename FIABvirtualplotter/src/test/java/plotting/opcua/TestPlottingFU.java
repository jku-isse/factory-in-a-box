package plotting.opcua;

import akka.actor.Actor;
import akka.actor.ActorSystem;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.opcua.server.OPCUABase;
import fiab.plotter.message.PlottingStatusUpdateEvent;
import fiab.plotter.plotting.opcua.functionalunit.PlotterFU;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutils.FUTestInfrastructure;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPlottingFU {

    //Playground
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        OPCUABase opcuaBase = OPCUABase.createAndStartLocalServer(4840, "TestPlotter");
        system.actorOf(PlotterFU.props(opcuaBase, opcuaBase.getRootNode(), WellknownPlotterCapability.SupportedColors.BLACK,
                new FUConnector(), new IntraMachineEventBus()));
    }

    private FUTestInfrastructure infrastructure;

    @BeforeEach
    public void setup() {
        infrastructure = new FUTestInfrastructure(4840);
        infrastructure.subscribeToIntraMachineEventBus();
        infrastructure.initializeActor(PlotterFU.props(infrastructure.getServer(), infrastructure.getServer().getRootNode(),
                WellknownPlotterCapability.SupportedColors.BLACK,
                new FUConnector(), infrastructure.getIntraMachineEventBus()), "TestPlotter");

        expectPlottingState(BasicMachineStates.STOPPED);
    }

    @AfterEach
    public void teardown() {
        infrastructure.shutdownInfrastructure();
    }

    @Test
    public void testResetAndStop() {

    }

    private void expectPlottingState(BasicMachineStates state) {
        PlottingStatusUpdateEvent event;
        event = infrastructure.getProbe().expectMsgClass(Duration.ofSeconds(10), PlottingStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }

    private static class PlottingOpcUaNodes {
        NodeId resetNode;
        NodeId stopNode;
        NodeId plotNode;
    }
}
