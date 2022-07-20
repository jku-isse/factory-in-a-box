package plotting;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.plotter.plotting.message.PlotImageRequest;
import fiab.plotter.plotting.message.PlottingStatusUpdateEvent;
import fiab.plotter.plotting.PlotterActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPlotterActor {

    private IntraMachineEventBus intraMachineEventBus;
    private FUConnector fuConnector;
    private ActorSystem system;
    private ActorRef plotActor;
    private TestKit probe;
    private String probeId;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create();
        intraMachineEventBus = new IntraMachineEventBus();
        fuConnector = new FUConnector();

        probe = new TestKit(system);
        probeId = probe.getRef().path().name();
        intraMachineEventBus.subscribe(probe.getRef(), new FUSubscriptionClassifier(probeId, "*"));

        plotActor = system.actorOf(PlotterActor.props(fuConnector, intraMachineEventBus));
        expectPlottingState(BasicMachineStates.STOPPED);
    }

    @AfterEach
    public void teardown() {
        intraMachineEventBus.unsubscribe(probe.getRef());
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testFullPlottingResetAndStop() {
        new TestKit(system) {
            {
                fuConnector.publish(new ResetRequest(probeId));
                expectPlottingState(BasicMachineStates.RESETTING);
                expectPlottingState(BasicMachineStates.IDLE);

                fuConnector.publish(new StopRequest(probeId));
                expectPlottingState(BasicMachineStates.STOPPING);
                expectPlottingState(BasicMachineStates.STOPPED);
            }
        };
    }

    @Test
    public void testPlotterFullCycle(){
        new TestKit(system) {
            {
                plotActor.tell(new ResetRequest(probeId), getRef());
                expectPlottingState(BasicMachineStates.RESETTING);
                expectPlottingState(BasicMachineStates.IDLE);

                plotActor.tell(new PlotImageRequest(probeId, "TestImg", "TestOrder"), getRef());
                expectPlottingState(BasicMachineStates.STARTING);
                expectPlottingState(BasicMachineStates.EXECUTE);
                expectPlottingState(BasicMachineStates.COMPLETING);
                expectPlottingState(BasicMachineStates.COMPLETE);

                plotActor.tell(new ResetRequest(probeId), getRef());
                expectPlottingState(BasicMachineStates.RESETTING);
                expectPlottingState(BasicMachineStates.IDLE);
            }
        };
    }

    private void expectPlottingState(BasicMachineStates state) {
        PlottingStatusUpdateEvent event;
        event = probe.expectMsgClass(Duration.ofSeconds(10),PlottingStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }

}
