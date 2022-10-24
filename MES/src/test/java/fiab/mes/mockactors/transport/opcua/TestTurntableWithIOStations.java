package fiab.mes.mockactors.transport.opcua;

import ProcessCore.ProcessStep;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.iostation.InputStationFactory;
import fiab.iostation.OutputStationFactory;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
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
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.plotter.PlotterFactory;
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

public class TestTurntableWithIOStations {

    private final Logger logger = LoggerFactory.getLogger(TestTurntableWithIOStations.class);

    MachineEventBus machineEventBus;
    ActorRef machineEventBusWrapper;
    ActorSystem system;
    ProcessStep step;

    public static void startupW34toE35() {
        // !!! Names are reflected in Nodeset, do not change without propagating to wiringinfo.json
        //StartupUtil.startupInputstation(0, "VirtualInputStation1");
        //StartupUtil.startupOutputstation(1, "VirtualOutputStation1");
        //fiab.machine.plotter.opcua.StartupUtil.startup(5, "VirtualPlotter31", SupportedColors.BLACK); //NORTH TT1
        //fiab.machine.plotter.opcua.StartupUtil.startup(6, "VirtualPlotter32", SupportedColors.BLACK); //NORTH TT2
        ActorSystem systemTT1 = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA1");
        //int portOffsetTT1 = 2;
        //boolean exposeInternalControls = false;
        //systemTT1.actorOf(OPCUATurntableRootActor.props("TurntableVirtualW34toN31toE21", portOffsetTT1, exposeInternalControls), "TurntableRoot");
        InputStationFactory.startStandaloneInputStation(systemTT1, 4840, "VirtualInputStation1");
        OutputStationFactory.startStandaloneOutputStation(systemTT1, 4841, "VirtualOutputStation1");
        PlotterFactory.startStandalonePlotter(systemTT1, 4845, "VirtualPlotter31", SupportedColors.BLACK);    //NORTH TT1
        PlotterFactory.startStandalonePlotter(systemTT1, 4846, "VirtualPlotter32", SupportedColors.BLACK);    //NORTH TT2
        TurntableFactory.startStandaloneTurntable(systemTT1, 4842, "TurntableRoot");
        ActorSystem systemTT2 = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA2");
        //int portOffsetTT2 = 3;
        //systemTT2.actorOf(OPCUATurntableRootActor.props("TurntableVirtualW20toN32toE35", portOffsetTT2, exposeInternalControls), "TurntableRoot");
        TurntableFactory.startStandaloneTurntable(systemTT2, 4843, "TurntableVirtualW20toN32toE35");

    }

    @BeforeEach
    void setup() {
        system = ActorSystem.create("TEST_ROOT_SYSTEM");
        // assume OPCUA server (mock or otherwise is started
        machineEventBus = new MachineEventBus();
        machineEventBusWrapper = system.actorOf(InterMachineEventBusWrapperActor.propsWithPreparedBus(machineEventBus), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @AfterEach
    void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    @Tag("IntegrationTest")
    void virtualIOandTT() {
        OrderProcess op = createRedGreenProcess();
        step = op.getAvailableSteps().get(0);
        Position posFrom = new Position("34");
        Position posTo = new Position("21");    //Since there is no second turntable, the output station uses this position
        new TestKit(system) {
            {
                ShopfloorLayout layout = new SingleTurntableLayout(system, machineEventBusWrapper);
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                layout.initializeAndDiscoverParticipantsForId(getRef(), INPUT_STATION, OUTPUT_STATION, TURNTABLE_1);

                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                boolean didReactOnIdle = false;
                boolean doRun = true;
                while (doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ServerHandshakeStatusUpdateEvent.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && !didReactOnIdle) {
                            logger.info("Sending TEST transport request to: " + msue.getMachineId());
                            TransportModuleRequest req = new TransportModuleRequest(machines.get(msue.getMachineId()), posFrom, posTo, "Order1", "TReq1");
                            machines.get(msue.getMachineId()).getAkkaActor().tell(req, getRef());
                            didReactOnIdle = true;
                        } else if (msue.getStatus().equals(BasicMachineStates.COMPLETE) || msue.getStatus().equals(BasicMachineStates.COMPLETING)) {
                            logger.info("Completing test upon receiving COMPLETE/ING from: " + msue.getMachineId());
                            doRun = false;
                        }
                    }
                }
            }
        };
    }

    @Test
    @Tag("IntegrationTest")
    void testHandoverWithVirtualIOStationsAndTTandVirtualPlotter() {
        OrderProcess op = createRedGreenProcess();
        step = op.getAvailableSteps().get(0);
        new TestKit(system) {
            {
                ShopfloorLayout layout = new SingleTurntableLayout(system, machineEventBusWrapper);
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                layout.initializeAndDiscoverParticipantsForId(getRef(), INPUT_STATION, OUTPUT_STATION, TURNTABLE_1, PLOTTER_RED);
                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();
                String ttMachineId = layout.getParticipantForId(TURNTABLE_1).getProxyMachineId();
                String plotterMachineId = layout.getParticipantForId(PLOTTER_RED).getProxyMachineId();
                String outputStationId = layout.getParticipantForId(OUTPUT_STATION).getProxyMachineId();
                boolean didReactOnIdle = false;
                boolean doRun = true;
                boolean plotterReady = false;
                boolean turntableReady = false;
                while (machines.size() < layout.getParticipants().size() || doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ReadyForProcessEvent.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        MachineConnectedEvent event = ((MachineConnectedEvent) te);
                        machines.put(event.getMachineId(), event.getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals(plotterMachineId)) {
                            sendPlotRegister(machines.get(msue.getMachineId()).getAkkaActor(), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals(ttMachineId)) {
                            turntableReady = true;
                        } else if (msue.getStatus().equals(BasicMachineStates.COMPLETING) &&
                                msue.getMachineId().equals(plotterMachineId)) {
                            //now do unloading
                            //sendTransportRequestNorth31ToEast21(machines.get(ttMachineId), getRef());
                            AkkaActorBackedCoreModelAbstractActor turntable = machines.get(ttMachineId);
                            TransportModuleRequest req = new TransportModuleRequest(turntable, new Position("37"), new Position("21"), "Order1", "TReq1");
                            turntable.getAkkaActor().tell(req, getRef());
                        }
                    }
                    if (te instanceof ReadyForProcessEvent) {
                        assert (((ReadyForProcessEvent) te).isReady());
                        plotterReady = true;
                        sendPlotRequest(machines.get(plotterMachineId).getAkkaActor(), getRef());
                    }

                    if (te instanceof IOStationStatusUpdateEvent) {
                        IOStationStatusUpdateEvent iosue = (IOStationStatusUpdateEvent) te;
                        if ((iosue.getStatus().equals(ServerSideStates.COMPLETE) || iosue.getStatus().equals(ServerSideStates.COMPLETING)) &&
                                iosue.getMachineId().equals(outputStationId)) {
                            logger.info("Completing test upon receiving COMPLETE/ING from: " + iosue.getMachineId());
                            doRun = false;
                        }
                    }
                    if (plotterReady && turntableReady && !didReactOnIdle) {
                        logger.info("Sending TEST transport request to Turntable1");
                        AkkaActorBackedCoreModelAbstractActor turntable = machines.get(ttMachineId);
                        TransportModuleRequest req = new TransportModuleRequest(turntable, new Position("37"), new Position("21"), "Order1", "TReq1");
                        turntable.getAkkaActor().tell(req, getRef());
                        didReactOnIdle = true;
                    }
                }
            }
        };
    }

    @Test
    @Tag("IntegrationTest")
    void testHandoverWithVirtualIOStationsAndTwoVirtualTTs() {
        OrderProcess op = create4ColorProcess();
        step = op.getAvailableSteps().get(0);
        new TestKit(system) {
            {
                ShopfloorLayout layout = new DefaultTestLayout(system, machineEventBusWrapper);
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                layout.initializeAndDiscoverParticipantsForId(getRef(),
                        INPUT_STATION, OUTPUT_STATION,
                        TURNTABLE_1, TURNTABLE_2,
                        PLOTTER_RED, PLOTTER_GREEN);
                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                String turntable1Id = layout.getParticipantForId(TURNTABLE_1).getProxyMachineId();
                String turntable2Id = layout.getParticipantForId(TURNTABLE_2).getProxyMachineId();
                String outputStationId = layout.getParticipantForId(OUTPUT_STATION).getProxyMachineId();
                boolean didReactOnIdle = false;
                boolean doRun = true;
                boolean turntableReady1 = false;
                boolean turntableReady2 = false;
                while (machines.size() < layout.getParticipants().size() || doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ReadyForProcessEvent.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals(turntable1Id)) {
                            turntableReady1 = true;
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals(turntable2Id)) {
                            turntableReady2 = true;
                        }
                    }
                    if (te instanceof IOStationStatusUpdateEvent) {
                        IOStationStatusUpdateEvent iosue = (IOStationStatusUpdateEvent) te;
                        if ((iosue.getStatus().equals(ServerSideStates.COMPLETE) || iosue.getStatus().equals(ServerSideStates.COMPLETING)) &&
                                iosue.getMachineId().equals(outputStationId)) {
                            logger.info("Completing test upon receiving COMPLETE/ING from: " + iosue.getMachineId());
                            doRun = false;
                        }
                    }
                    if (turntableReady1 && turntableReady2 && !didReactOnIdle) {
                        logger.info("Sending TEST transport request to Turntable1");
                        AkkaActorBackedCoreModelAbstractActor tt1 = machines.get(turntable1Id);
                        TransportModuleRequest req = new TransportModuleRequest(tt1, new Position("34"), new Position("21"), "Order1", "TReq1");
                        tt1.getAkkaActor().tell(req, getRef());
                        logger.info("Sending TEST transport request to Turntable2");
                        AkkaActorBackedCoreModelAbstractActor tt2 = machines.get(turntable2Id);
                        TransportModuleRequest req2 = new TransportModuleRequest(tt2, new Position("20"), new Position("35"), "Order1", "TReq2");
                        tt2.getAkkaActor().tell(req2, getRef());
                        didReactOnIdle = true;
                    }
                }
            }
        };
    }


    //	@Test  //FIXME: hardware centric not ok
//  @Tag("SystemTest")
//	void realIOandRealSingleTTAndPLotter() {
//		Set<String> urlsToBrowse = new HashSet<String>();
//		urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34 west inputstation
//		urlsToBrowse.add("opc.tcp://192.168.0.35:4840");	// POS EAST 35/ outputstation				
//		urlsToBrowse.add("opc.tcp://192.168.0.31:4840");	// POS NORTH 31/ plotter 1
//		urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");		// Pos20 TT
//		Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
//		ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
//		Position posFrom = new Position("34");
//		Position posTo = new Position("31");
//		runTransportTestWith(capURI2Spawning, urlsToBrowse, posFrom, posTo);
//	}

    @Test  //Works somewhat
    @Tag("SystemTest")
    void realIOandRealSingleTT() {
        ShopfloorLayout layout = new SingleTurntableLayout(system, machineEventBusWrapper);
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34 west inputstation
        urlsToBrowse.add("opc.tcp://192.168.0.35:4840");    // POS EAST 35/ outputstation
        urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");        // Pos20 TT
        Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
        ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
        Position posFrom = new Position("34");
        Position posTo = new Position("35");
        runTransportTestWith(posFrom, posTo);
    }

    //FIXME this will not work anymore, but only used for one system test.
    private boolean runTransportTestWith(Position posFrom, Position posTo) {
        new TestKit(system) {
            {
                ShopfloorLayout layout = new SingleTurntableLayout(system, machineEventBusWrapper);
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");

                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                boolean didReactOnIdle = false;
                boolean doRun = true;
                while (doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ServerHandshakeStatusUpdateEvent.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE) && !didReactOnIdle) {
                            logger.info("Sending TEST transport request to: " + msue.getMachineId());
                            TransportModuleRequest req = new TransportModuleRequest(machines.get(msue.getMachineId()), posFrom, posTo, "Order1", "TReq1");
                            machines.get(msue.getMachineId()).getAkkaActor().tell(req, getRef());
                            didReactOnIdle = true;
                        } else if (msue.getStatus().equals(BasicMachineStates.COMPLETE) || msue.getStatus().equals(BasicMachineStates.COMPLETING)) {
                            logger.info("Completing test upon receiving COMPLETE/ING from: " + msue.getMachineId());
                            doRun = false;
                        }
                    }
                }
            }
        };
        return true;
    }

    private OrderProcess create4ColorProcess() {
        //BLACK, BLUE, GREEN, RED
        ProcessCore.Process p = ProduceProcess.getSequential4ColorProcess("P1-");
        OrderProcess op = new OrderProcess(p);
        op.activateProcess();
        return op;
    }

    private OrderProcess createRedGreenProcess() {
        ProcessCore.Process p = ProduceProcess.getRedAndGreenStepProcess("P1-");
        OrderProcess op = new OrderProcess(p);
        op.activateProcess();
        return op;
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
    }

}
