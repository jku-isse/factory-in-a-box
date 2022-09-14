package fiab.mes.assembly;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.assembly.monitoring.actor.AssemblyMonitoringActor;
import fiab.mes.assembly.order.actor.BikeAssemblyOrderPlanningActor;
import fiab.mes.assembly.transport.actor.DummyTransportSystemCoordinatorActor;
import fiab.mes.eventbus.*;
import fiab.mes.machine.MachineEntryActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.shopfloor.DefaultLayout;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.plotter.PlotterFactory;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBikeAssemblyInfrastructure {

    ActorSystem system;
    protected MachineEventBus machineEventBus;
    protected ActorRef orderEventBus;
    protected ActorRef monitorEventBus;
    protected ActorRef orderPlanningActor;
    protected ActorRef orderEntryActor;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("BikeTestActorSystem");
        //BikeAssemblyInfrastructure shopfloor = new BikeAssemblyInfrastructure(system);
        orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        machineEventBus = new MachineEventBus();
        ActorRef machineEventBusWrapper = system.actorOf(InterMachineEventBusWrapperActor.propsWithPreparedBus(machineEventBus), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        monitorEventBus = system.actorOf(AssemblyMonitoringEventBusWrapperActor.props(), AssemblyMonitoringEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);

        ActorRef transportActor = system.actorOf(DummyTransportSystemCoordinatorActor.props(), DummyTransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef monitoringActor = system.actorOf(AssemblyMonitoringActor.props(), AssemblyMonitoringActor.WELLKNOWN_LOOKUP_NAME);
        orderPlanningActor = system.actorOf(BikeAssemblyOrderPlanningActor.props(), BikeAssemblyOrderPlanningActor.WELLKNOWN_LOOKUP_NAME);

        orderEntryActor = system.actorOf(OrderEntryActor.props(), OrderEntryActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef machineEntryActor = system.actorOf(MachineEntryActor.props(), MachineEntryActor.WELLKNOWN_LOOKUP_NAME);
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    @Tag("IntegrationTest")
    public void testMessageFromOrderActorToPlaner() {
        new TestKit(system) {
            {
                new DefaultLayout(system, false).setupIOStations(34, 35);
                PlotterFactory.startupStandalonePlotter(4840, machineEventBus, "");
                monitorEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());
                RegisterProcessRequest req = createSinglePrintRedOrder("Test", getRef());
                orderEntryActor.tell(req, getRef());
                //expectMsgClass(Duration.ofSeconds(10), OrderEvent.class);
                String str = expectMsgAnyClassOf(String.class); //Apparently we forward a String?
                assertEquals(req.getRootOrderId(), str);
            }
        };
    }

    @Test
    @Tag("IntegrationTest")
    public void testMessageFromPlanerToOrderActor() {

    }

    public RegisterProcessRequest createSinglePrintRedOrder(String oid, ActorRef testProbe) {
        OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleRedStepProcess(oid));
        RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
        return req;
    }
}
