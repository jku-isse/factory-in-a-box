package fiab.mes.mockactors.transport.coordinator.opcua;

import ProcessCore.ProcessStep;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.iostation.InputStationFactory;
import fiab.iostation.OutputStationFactory;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.shopfloor.layout.DefaultTestLayout;
import fiab.mes.shopfloor.layout.ShopfloorLayout;
import fiab.mes.shopfloor.layout.SingleTurntableLayout;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.turntable.TurntableFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

import static fiab.mes.shopfloor.utils.ShopfloorUtils.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestTransportSystemCoordinatorActorViaOPCUA {

    private final Logger logger = LoggerFactory.getLogger(TestTransportSystemCoordinatorActorViaOPCUA.class);

    protected ActorSystem system;
    public String ROOT_SYSTEM = "TEST_TRANSPORTSYSTEM";
    protected ActorRef orderEventBus;
    protected ActorRef machineEventBus;
    protected ActorRef coordActor;
    protected ProcessStep step;

    HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();

    @BeforeEach
    public void setup() throws Exception {
        system = ActorSystem.create(ROOT_SYSTEM);
        orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ProcessCore.Process p = ProduceProcess.getSingleRedStepProcess("P1-");
        OrderProcess op = new OrderProcess(p);
        op.activateProcess();
        step = op.getAvailableSteps().get(0);
    }

    @AfterEach
    public void teardown() {
        knownActors.clear();
        TestKit.shutdownActorSystem(system);
    }

    @Test
    @Tag("IntegrationTest")
    void testVirtualIOandTT() {
        new TestKit(system) {
            {
                ShopfloorLayout layout = new SingleTurntableLayout(system, machineEventBus);
                layout.subscribeToInterMachineEventBus(getRef(), getRef().path().name());
                layout.initializeAndDiscoverParticipantsForId(getRef(), INPUT_STATION, OUTPUT_STATION, TURNTABLE_1);
                Map<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
                coordActor = system.actorOf(TransportSystemCoordinatorActor.props(layout.getTransportRoutingAndMapping(), layout.getTransportPositionLookup(), layout.getAmountOfTransportUnits()), "TransportCoordinator");
                while (machines.size() < layout.getParticipants().size()) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, TransportSystemStatusMessage.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        MachineConnectedEvent connectedEvent = ((MachineConnectedEvent) te);
                        machines.put(connectedEvent.getMachineId(), connectedEvent.getMachine());
                    }
                }
                assertTrue(runTransportFromInputToOutputStationSuccessful(this, layout, machines));
            }
        };
    }

    @Test
    @Tag("IntegrationTest")
    void virtualIOandTwoTTs() {
        new TestKit(system) {
            {
                ShopfloorLayout layout = new DefaultTestLayout(system, machineEventBus);
                layout.subscribeToInterMachineEventBus(getRef(), getRef().path().name());
                layout.initializeAndDiscoverParticipantsForId(getRef(), INPUT_STATION, OUTPUT_STATION, TURNTABLE_1, TURNTABLE_2);
                Map<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
                coordActor = system.actorOf(TransportSystemCoordinatorActor.props(layout.getTransportRoutingAndMapping(), layout.getTransportPositionLookup(), layout.getAmountOfTransportUnits()), "TransportCoordinator");
                while (machines.size() < layout.getParticipants().size()) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, TransportSystemStatusMessage.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        MachineConnectedEvent connectedEvent = ((MachineConnectedEvent) te);
                        machines.put(connectedEvent.getMachineId(), connectedEvent.getMachine());
                    }
                }
                assertTrue(runTransportFromInputToOutputStationSuccessful(this, layout, machines));
            }
        };
    }

    @Test
    @Tag("IntegrationTest")
    void testHandoverWithVirtualIOStationsAndTTandVirtualPlotter() {
        new TestKit(system) {
            {
                ShopfloorLayout layout = new SingleTurntableLayout(system, machineEventBus);
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                coordActor = system.actorOf(TransportSystemCoordinatorActor.props(layout.getTransportRoutingAndMapping(), layout.getTransportPositionLookup(), 1), "TransportCoordinator");
                layout.initializeAndDiscoverParticipantsForId(getRef(), INPUT_STATION, OUTPUT_STATION, TURNTABLE_1, PLOTTER_RED);

                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                boolean didReactOnIdle = false;
                boolean doRun = true;
                boolean plotterReady = false;
                boolean turntableReady = false;
                while (machines.size() < layout.getParticipants().size() || doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ReadyForProcessEvent.class, RegisterTransportRequestStatusResponse.class, TransportSystemStatusMessage.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    String plotterId = layout.getParticipantForId(PLOTTER_RED).getProxyMachineId();
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals(plotterId)) {
                            sendPlotRegister(machines.get(msue.getMachineId()).getAkkaActor(), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals(layout.getParticipantForId(TURNTABLE_1).getProxyMachineId())) {
                            turntableReady = true;
                        } else if (msue.getStatus().equals(BasicMachineStates.COMPLETING) &&
                                msue.getMachineId().equals(plotterId)) {
                            //now do unloading
                            RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get(plotterId), knownActors.get(layout.getParticipantForId(OUTPUT_STATION).getProxyMachineId()), "TestOrder1", getRef());
                            coordActor.tell(rtr, getRef());
                        }
                    }
                    if (te instanceof ReadyForProcessEvent) {
                        assert (((ReadyForProcessEvent) te).isReady());
                        plotterReady = true;
                        sendPlotRequest(machines.get(plotterId).getAkkaActor(), getRef());
                    }

                    if (te instanceof IOStationStatusUpdateEvent) {
                        IOStationStatusUpdateEvent iosue = (IOStationStatusUpdateEvent) te;
                        if ((iosue.getStatus().equals(ServerSideStates.COMPLETE) || iosue.getStatus().equals(ServerSideStates.COMPLETING)) &&
                                iosue.getMachineId().equals(layout.getParticipantForId(OUTPUT_STATION).getProxyMachineId())) {
                            logger.info("Completing test upon receiving COMPLETE/ING from: " + iosue.getMachineId());
                            doRun = false;
                        }
                    }
                    if (plotterReady && turntableReady && !didReactOnIdle) {
                        logger.info("Sending TEST transport request to Turntable1");
                        RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get(layout.getParticipantForId(INPUT_STATION).getProxyMachineId()), knownActors.get(plotterId), "TestOrder1", getRef());
                        coordActor.tell(rtr, getRef());
                        didReactOnIdle = true;
                    }
                }
            }
        };
    }


    @Test //FIXME find way to get IDs for ShopfloorUtils identifiers
    @Tag("SystemTest")
    void testHandoverWithRealIOStationsAndTTandPlotter() {
        new TestKit(system) {
            {
                Set<String> urlsToBrowse = new HashSet<String>();
                urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34 west inputstation
                urlsToBrowse.add("opc.tcp://192.168.0.31:4840"); //Pos31 north plotter
                urlsToBrowse.add("opc.tcp://192.168.0.35:4840");    // POS EAST 35/ outputstation
                urlsToBrowse.add("opc.tcp://192.168.0.20:4842");        // Pos20 TT

                ShopfloorLayout layout = new SingleTurntableLayout(system, machineEventBus);
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                coordActor = system.actorOf(TransportSystemCoordinatorActor.props(layout.getTransportRoutingAndMapping(), layout.getTransportPositionLookup(), 1), "TransportCoordinator");

                layout.runRemoteDiscovery(getRef(), urlsToBrowse);
                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                boolean didReactOnIdle = false;
                boolean doRun = true;
                boolean plotterReady = false;
                boolean turntableReady = false;
                while (machines.size() < urlsToBrowse.size() || doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ReadyForProcessEvent.class, RegisterTransportRequestStatusResponse.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("opc.tcp://localhost:4845/VirtualPlotter31/Plotting_FU")) {
                            sendPlotRegister(machines.get(msue.getMachineId()).getAkkaActor(), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("Turntable1/Turntable_FU")) {
                            turntableReady = true;
                        } else if (msue.getStatus().equals(BasicMachineStates.COMPLETING) &&
                                msue.getMachineId().equals("opc.tcp://localhost:4845/VirtualPlotter31/Plotting_FU")) {
                            //now do unloading
                            RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("opc.tcp://localhost:4845/VirtualPlotter31/Plotting_FU"), knownActors.get("VirtualOutputStation1/IOSTATION"), "TestOrder1", getRef());
                            coordActor.tell(rtr, getRef());
                        }
                    }
                    if (te instanceof ReadyForProcessEvent) {
                        assert (((ReadyForProcessEvent) te).isReady());
                        plotterReady = true;
                        sendPlotRequest(machines.get("opc.tcp://localhost:4845/VirtualPlotter31/Plotting_FU").getAkkaActor(), getRef());
                    }

                    if (te instanceof IOStationStatusUpdateEvent) {
                        IOStationStatusUpdateEvent iosue = (IOStationStatusUpdateEvent) te;
                        if ((iosue.getStatus().equals(ServerSideStates.COMPLETE) || iosue.getStatus().equals(ServerSideStates.COMPLETING)) &&
                                iosue.getMachineId().equals("VirtualOutputStation1/IOSTATION")) {
                            logger.info("Completing test upon receiving COMPLETE/ING from: " + iosue.getMachineId());
                            doRun = false;
                        }
                    }
                    if (plotterReady && turntableReady && !didReactOnIdle) {
                        logger.info("Sending TEST transport request to Turntable1");
                        RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("VirtualInputStation1/IOSTATION"), knownActors.get("opc.tcp://localhost:4845/VirtualPlotter31/Plotting_FU"), "TestOrder1", getRef());
                        coordActor.tell(rtr, getRef());
                        didReactOnIdle = true;
                    }
                }
            }
        };
    }

    private boolean runTransportFromInputToOutputStationSuccessful(TestKit testKit, ShopfloorLayout layout, Map<String, AkkaActorBackedCoreModelAbstractActor> machines) {
        boolean transportSuccessful = false, transportIssued = false;
        boolean turntableIdle = false, inputIdle = false, outputIdle = false;
        while (!transportSuccessful) {
            TimedEvent te = testKit.expectMsgAnyClassOf(Duration.ofSeconds(15), IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class, TransportSystemStatusMessage.class);
            logEvent(te);
            if (te instanceof MachineStatusUpdateEvent) {
                MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                    machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), testKit.getRef());
                } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && !transportIssued) {
                    logger.info("Sending TEST transport request to: " + msue.getMachineId());
                    String inputStationId = layout.getParticipantForId(INPUT_STATION).getProxyMachineId();
                    String outputStationId = layout.getParticipantForId(OUTPUT_STATION).getProxyMachineId();
                    RegisterTransportRequest rtr = new RegisterTransportRequest(machines.get(inputStationId), machines.get(outputStationId), "TestOrder1", testKit.getRef());
                    coordActor.tell(rtr, testKit.getRef());
                    transportIssued = true;
                }
            }
            if (te instanceof IOStationStatusUpdateEvent) {
                IOStationStatusUpdateEvent ioEvent = (IOStationStatusUpdateEvent) te;
                ServerSideStates status = ioEvent.getStatus();
                if (status.equals(ServerSideStates.STOPPED)) {
                    machines.get(ioEvent.getMachineId()).getAkkaActor()
                            .tell(new GenericMachineRequests.Reset(testKit.getRef().path().name()), testKit.getRef());
                }
            }
            if (te instanceof RegisterTransportRequestStatusResponse) {
                RegisterTransportRequestStatusResponse rtrr = (RegisterTransportRequestStatusResponse) te;
                if (rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.COMPLETED)) {
                    transportSuccessful = true;
                } else {
                    assertTrue(rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.QUEUED) ||
                            rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.ISSUED));
                }
            }
        }
        return true;
    }

    private void sendPlotRequest(ActorRef plotter, ActorRef self) {
        LockForOrder lfo = new LockForOrder("Step1", "Order1");
        plotter.tell(lfo, self);
    }

    private void sendPlotRegister(ActorRef plotter, ActorRef self) {
        RegisterProcessStepRequest req = new RegisterProcessStepRequest("Order1", "Step1", step, self);
        plotter.tell(req, self);
    }

    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
        if (event instanceof MachineConnectedEvent) {
            knownActors.put(((MachineConnectedEvent) event).getMachineId(), ((MachineConnectedEvent) event).getMachine());
        }
    }

}
