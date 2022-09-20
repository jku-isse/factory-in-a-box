package fiab.mes.mockactors.stopOrReset;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.mockactors.iostation.VirtualIOStationActorFactory;
import fiab.mes.shopfloor.DefaultTestLayout;
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
import fiab.mes.shopfloor.DefaultLayout;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;

import static fiab.mes.shopfloor.utils.ShopfloorUtils.PLOTTER_GREEN;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.PLOTTER_RED;

@Tag("IntegrationTest")
class TestUaUaStopMachineTurntablePlotter {

    protected static ActorSystem system;
    public static String ROOT_SYSTEM = "routes";
    protected static ActorRef machineEventBus;
    protected static ActorRef orderEventBus;
    protected static ActorRef orderPlanningActor;
    protected static ActorRef coordActor;
    //protected static DefaultLayout layout;
    protected static DefaultTestLayout layout;

    private static final Logger logger = LoggerFactory.getLogger(TestUaUaStopMachineTurntablePlotter.class);
    static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();

    @BeforeEach
    public void setUp() throws Exception {
        // setup shopfloor
        // setup machines
        // setup processes
        // setup order actors?
        // add processes to orderplanning actor
        system = ActorSystem.create(ROOT_SYSTEM);
        //HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
        //DefaultTransportPositionLookup dns = new DefaultTransportPositionLookup();
        //layout = new DefaultLayout(system);
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
        system = null;
    }

    @Test
    void testStopMachineWhenUnassigned() {
        new TestKit(system) {
            {
                //layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
                //layout.setupTwoTurntableWith2MachinesAndIO();
                layout.subscribeToMachineEventBus(getRef(), "Tester");
                layout.initializeDefaultLayoutWithProxies();
                layout.resetParticipants();
                int countConnEvents = 0;
                boolean isPlannerFunctional = false;
                while (!isPlannerFunctional || countConnEvents < 8) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class, TransportSystemStatusMessage.class);
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

                String unassignedMachineId = PLOTTER_GREEN;

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
                            knownActors.get(unassignedMachineId).getAkkaActor().tell(new GenericMachineRequests.Stop(unassignedMachineId), getRef());
                            sentStop = true;
                        }
                        if (oe.getEventType().equals(OrderEvent.OrderEventType.COMPLETED)) {
                            System.out.println(" ---------------- Order complete: " + oe.getOrderId());
                            order1Done = true;
                        }
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED) && !sentStop)
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
        //FIXME: stopping is sent and acknoledged, but order is correctly processed til end, nevertheless
    void testStopMachineWhenAssigned() throws Exception {
        new TestKit(system) {
            {
                //layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
                //layout.setupTwoTurntableWith2MachinesAndIO();
                layout.subscribeToMachineEventBus(getRef(), "Tester");
                layout.initializeDefaultLayout();
                int countConnEvents = 0;
                boolean isPlannerFunctional = false;
                while (!isPlannerFunctional || countConnEvents < 8) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class);
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

                String assignedMachineId = "MockMachineActor31";

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

    @Test
    void testStopMachineWhenPlotting() {
        new TestKit(system) {
            {
                //layout.eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
                //layout.setupTwoTurntableWith2MachinesAndIO();
                layout.subscribeToMachineEventBus(getRef(), "Tester");
                layout.initializeDefaultLayoutWithProxies();
                int countConnEvents = 0;
                boolean isPlannerFunctional = false;
                while (!isPlannerFunctional || countConnEvents < 8) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class, TransportSystemStatusMessage.class);
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

                String assignedMachineId = PLOTTER_RED;

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


    public OrderProcess subscribeAndRegisterSinglePrintRedOrder(String oid, ActorRef testProbe) {
        orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe);
        OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleRedStepProcess(oid));
        RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
        orderPlanningActor.tell(req, testProbe);
        return op1;
    }

    public void subscribeAndRegisterSinglePrintGreenOrder(String oid, ActorRef testProbe) {
        orderEventBus.tell(new SubscribeMessage(testProbe, new MESSubscriptionClassifier("OrderMock", oid)), testProbe);
        OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleGreenStepProcess(oid));
        RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, testProbe);
        orderPlanningActor.tell(req, testProbe);
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
        System.out.println(event);   //FIXME remove this
    }

//	public static ActorRef getMachineMockActor(int id, SupportedColors color) {
//		ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
//		Actor modelActor = TestMockMachineActor.getDefaultMachineActor(id);
//		AbstractCapability cap = WellknownPlotterCapability.getColorPlottingCapability(color);
//		return system.actorOf(MockMachineActor.props(eventBusByRef, cap, modelActor));
//	}

    private boolean matches(OrderEvent e, String orderId, OrderEventType type) {
        return (e.getEventType().equals(type) && e.getOrderId().equals(orderId));
    }

}
