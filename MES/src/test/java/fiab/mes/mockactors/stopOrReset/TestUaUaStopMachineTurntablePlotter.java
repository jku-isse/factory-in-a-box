package fiab.mes.mockactors.stopOrReset;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.shopfloor.layout.DefaultTestLayout;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;

import static fiab.mes.shopfloor.utils.ShopfloorUtils.PLOTTER_GREEN;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.PLOTTER_RED;

@Tag("IntegrationTest")
class TestUaUaStopMachineTurntablePlotter {

    protected ActorSystem system;
    public String ROOT_SYSTEM = "routes";
    protected ActorRef machineEventBus;
    protected ActorRef orderEventBus;
    protected ActorRef orderPlanningActor;
    protected ActorRef coordActor;
    //protected static DefaultLayout layout;
    protected DefaultTestLayout layout;

    private final Logger logger = LoggerFactory.getLogger(TestUaUaStopMachineTurntablePlotter.class);
    HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();

    @BeforeEach
    public void setUp() throws Exception {
        // setup shopfloor
        // setup machines
        // setup processes
        // setup order actors?
        // add processes to orderplanning actor
        system = ActorSystem.create(ROOT_SYSTEM);
        ActorRef interMachineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        layout = new DefaultTestLayout(system, interMachineEventBus);
        orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        coordActor = system.actorOf(TransportSystemCoordinatorActor.props(layout.getTransportRoutingAndMapping(), layout.getTransportPositionLookup(), 2), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        orderPlanningActor = system.actorOf(OrderPlanningActor.props(), OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);

    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        knownActors.clear();
    }

    @Test   //FIXME only passes reliably when run individually
    void testStopMachineWhenUnassigned() {
        new TestKit(system) {
            {
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                layout.initializeAndDiscoverParticipants(getRef());
                int countConnEvents = 0;
                boolean isPlannerFunctional = false;
                while (!isPlannerFunctional || countConnEvents < 8) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class, TransportSystemStatusMessage.class);
                    logEvent(te);
                    if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
                        isPlannerFunctional = true;
                    }
                    if (te instanceof MachineConnectedEvent) {
                        countConnEvents++;
                        knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                    }
                }

                String unassignedMachineId = layout.getParticipantForId(PLOTTER_GREEN).getProxyMachineId();

                String oid1 = "Order1";
                subscribeAndRegisterPrintGreenAndRedOrder(oid1, getRef());
                boolean order1Done = false;
                boolean sentStop = false;
                boolean machineStopped = false;
                while (!order1Done || !machineStopped) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), TimedEvent.class);
                    logEvent(te);
                    if (te instanceof OrderEvent) {
                        OrderEvent oe = (OrderEvent) te;
                        if (matches(oe, oid1, OrderEventType.ALLOCATED) && !sentStop) {
                            knownActors.get(unassignedMachineId).getAkkaActor().tell(new GenericMachineRequests.Stop(unassignedMachineId), getRef());
                            sentStop = true;
                        }
                        if (oe.getEventType().equals(OrderEvent.OrderEventType.COMPLETED)) {
                            System.out.println(" ---------------- Order complete: " + oe.getOrderId());
                            order1Done = true;
                        }
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED) /*&& !sentStop*/)
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPING) && ((MachineStatusUpdateEvent) te).getMachineId().equals(unassignedMachineId)) {
                            machineStopped = true;
                        }
                    }
                }

            }
        };
    }

    @Test
    void testStopMachineWhenAssigned() {
        new TestKit(system) {
            {
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                layout.initializeAndDiscoverParticipants(getRef());
                int countConnEvents = 0;
                boolean isPlannerFunctional = false;
                while (!isPlannerFunctional || countConnEvents < 8) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class, TransportSystemStatusMessage.class);
                    logEvent(te);
                    if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
                        isPlannerFunctional = true;
                    }
                    if (te instanceof MachineConnectedEvent) {
                        countConnEvents++;
                        knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                    }
                }

                String assignedMachineId = layout.getParticipantForId(PLOTTER_RED).getProxyMachineId();

                String oid1 = "Order1";
                subscribeAndRegisterPrintGreenAndRedOrder(oid1, getRef());
                boolean order1Done = false;
                boolean sentStop = false;
                boolean machineStopped = false;
                while (!order1Done || !machineStopped) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class);
                    logEvent(te);
                    if (te instanceof OrderEvent) {
                        OrderEvent oe = (OrderEvent) te;
                        if (matches(oe, oid1, OrderEventType.ALLOCATED) && !sentStop) {
                            knownActors.get(assignedMachineId).getAkkaActor().tell(new GenericMachineRequests.Stop(assignedMachineId), getRef());
                            sentStop = true;
                        }
                        if (oe.getEventType().equals(OrderEvent.OrderEventType.PREMATURE_REMOVAL)) {
                            System.out.println(" ---------------- Order premature removal due to machine stop: " + oe.getOrderId());
                            order1Done = true;
                        }
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED) && !sentStop)
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPING) && ((MachineStatusUpdateEvent) te).getMachineId().equals(assignedMachineId)) {
                            machineStopped = true;
                        }
                    }
                }

            }
        };
    }

    @Test   //FIXME only passes reliably when run individually
    void testStopMachineWhenPlotting() {
        new TestKit(system) {
            {
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                layout.initializeAndDiscoverParticipants(getRef());
                int countConnEvents = 0;
                boolean isPlannerFunctional = false;
                while (!isPlannerFunctional || countConnEvents < 8) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class, TransportSystemStatusMessage.class);
                    logEvent(te);
                    if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
                        isPlannerFunctional = true;
                    }
                    if (te instanceof MachineConnectedEvent) {
                        countConnEvents++;
                        knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                    }
                }

                String assignedMachineId = layout.getParticipantForId(PLOTTER_RED).getProxyMachineId();

                String oid1 = "Order1";
                subscribeAndRegisterPrintGreenAndRedOrder(oid1, getRef());
                boolean order1Done = false;
                boolean sentStop = false;
                boolean machineStopped = false;
                while (!order1Done || !machineStopped) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), TimedEvent.class);
                    logEvent(te);
                    if (te instanceof OrderEvent) {
                        OrderEvent oe = (OrderEvent) te;
//						if (matches(oe, oid1, OrderEventType.PRODUCING) && !sentStop) {
//							knownActors.get(assignedMachineId).getAkkaActor().tell(new GenericMachineRequests.Stop(assignedMachineId), getRef());
//							sentStop = true;
//						}
                        if (oe.getEventType().equals(OrderEvent.OrderEventType.PREMATURE_REMOVAL)) {
                            System.out.println(" ---------------- Order premature removal due to machine stop: " + oe.getOrderId());
                            order1Done = true;
                        }
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.EXECUTE) && ((MachineStatusUpdateEvent) te).getMachineId().equals(assignedMachineId) && !sentStop) {
                            knownActors.get(assignedMachineId).getAkkaActor().tell(new GenericMachineRequests.Stop(assignedMachineId), getRef());
                            sentStop = true;
                        }
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED) && !sentStop)
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPING) && ((MachineStatusUpdateEvent) te).getMachineId().equals(assignedMachineId)) {
                            machineStopped = true;
                        }
                    }
                }

            }
        };
    }

    public OrderProcess subscribeAndRegisterPrintGreenAndRedOrder(String oid, ActorRef testProbe) {
        orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe);
        OrderProcess op1 = new OrderProcess(ProduceProcess.getRedAndGreenStepProcess(oid));
        RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
        orderPlanningActor.tell(req, testProbe);
        return op1;
    }

    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }

    private boolean matches(OrderEvent e, String orderId, OrderEventType type) {
        return (e.getEventType().equals(type) && e.getOrderId().equals(orderId));
    }

}
