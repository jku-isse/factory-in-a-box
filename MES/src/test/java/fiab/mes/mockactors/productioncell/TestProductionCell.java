package fiab.mes.mockactors.productioncell;

import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.ProcessStep;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.capabilities.plotting.EcoreProcessUtils;
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
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.opcua.CapabilityImplementationMetadata;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fiab.mes.productioncell.foldingstation.FoldingProductionCellCoordinator;
import fiab.mes.productioncell.foldingstation.DefaultFoldingCellTransportPositionLookup;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestProductionCell {

    public static String ROOT_SYSTEM = "routes";
    protected static ActorSystem system;

    protected static ActorRef foldingCellCoord;
    protected static ActorRef transportCoord;
    protected static ActorRef machineEventBus;

    private static final Logger logger = LoggerFactory.getLogger(TestProductionCell.class);
    static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();

    @BeforeAll
    public static void setUpBeforeClass() {
        system = ActorSystem.create(ROOT_SYSTEM);
        HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
        DefaultFoldingCellTransportPositionLookup dns = new DefaultFoldingCellTransportPositionLookup();
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        transportCoord = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        foldingCellCoord = system.actorOf(FoldingProductionCellCoordinator.props(), FoldingProductionCellCoordinator.WELLKNOWN_LOOKUP_NAME);
    }

    @AfterAll
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @BeforeEach
    public void setupBeforeEach() {
        knownActors.clear();
    }

    @Test
    void testProductionCellDiscovery() throws Exception {
        new TestKit(system) {
            {
                //Set<String> urlsToBrowse = getLocalhostLayout();  //Used for local mock machines
                Set<String> urlsToBrowse = getTestLayout();

                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
                ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                urlsToBrowse.forEach(url -> {
                    ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
                    discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
                });

                int countConnEvents = 0;
                Set<String> respondingMachines = new HashSet<>();
                boolean isPlannerFunctional = false;
                boolean isTransportFunctional = false;
                while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() || !isTransportFunctional || respondingMachines.size() < urlsToBrowse.size()) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class);
                    logEvent(te);
                    if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlanerStatusMessage.PlannerState.FULLY_OPERATIONAL)) {
                        isPlannerFunctional = true;
                    }
                    if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState().equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
                        isTransportFunctional = true;
                    }
                    if (te instanceof MachineConnectedEvent) {
                        countConnEvents++;
                        knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        respondingMachines.add(((MachineStatusUpdateEvent) te).getMachineId());
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                    }
                    if (te instanceof IOStationStatusUpdateEvent) {
                        respondingMachines.add(((IOStationStatusUpdateEvent) te).getMachineId());
                    }
                }
                assertTrue(isPlannerFunctional);
                assertTrue(isTransportFunctional);
                assertEquals(countConnEvents, urlsToBrowse.size());
                assertEquals(respondingMachines.size(), urlsToBrowse.size());
            }
        };
    }

    @Test
    void testProductionCellTransportToOneFoldingStation() throws Exception {
        new TestKit(system) {
            {
                Set<String> urlsToBrowse = getTestLayout();
                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<>();
                ShopfloorConfigurations.addSpawners(capURI2Spawning, new DefaultFoldingCellTransportPositionLookup());
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                urlsToBrowse.forEach(url -> {
                    ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
                    discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
                });

                CapabilityInvocation foldingCap = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
                foldingCap.setID("TestFoldingCapabilityId");
                foldingCap.setDisplayName("TestFoldingCapability");
                foldingCap.setInvokedCapability(WellknownFoldingCapability.getFoldingShapeCapability());
                foldingCap.getInputMappings().add(EcoreProcessUtils.getVariableMapping(WellknownFoldingCapability.getShapeInputParameter()));

                ProcessCore.Process proc = ProcessCoreFactory.eINSTANCE.createProcess();
                proc.setDisplayName("ProcessTemplate4Folds");
                proc.setID("ProcessTemplate4Folds");
                EcoreProcessUtils.addProcessvariables(proc, "Box");
                EcoreProcessUtils.mapCapInputToProcessVar(proc.getVariables(), foldingCap);
                proc.getSteps().add(foldingCap);

                OrderProcess op = new OrderProcess(proc);
                op.activateProcess();

                ProcessStep step = op.getAvailableSteps().get(0);

                int countConnEvents = 0;
                boolean isPlannerFunctional = false;
                boolean isTransportFunctional = false;
                boolean foldingComplete = false;
                boolean isProcessAssigned = false;
                while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() || !isTransportFunctional || !foldingComplete || !isProcessAssigned) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofMinutes(10), TimedEvent.class);
                    logEvent(te);
                    if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlanerStatusMessage.PlannerState.FULLY_OPERATIONAL)) {
                        isPlannerFunctional = true;
                    }
                    if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState().equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
                        isTransportFunctional = true;
                    }
                    if (te instanceof MachineConnectedEvent) {
                        countConnEvents++;
                        knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                        logger.info("#%# Machine connected: " + ((MachineConnectedEvent) te).getMachineId() + " and stored in: "
                                + knownActors.get(((MachineConnectedEvent) te).getMachineId()));
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED)) {
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );

                        }
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.IDLE)) {
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId()))
                                    .filter(m -> m.getId().toLowerCase().contains("Folding".toLowerCase()))
                                    .ifPresent(actor -> actor.getAkkaActor().tell(new RegisterProcessStepRequest("Test", step.getID(), step, getRef()), getRef()));
                            //FIXME send correct request to foldingstation
                            //Turntable is assigned transport even though folding station is not ready?
                        }
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.COMPLETE)) {
                            boolean isFoldingStation = Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId()))
                                    .filter(m -> m.getId().toLowerCase().contains("Folding".toLowerCase())).isPresent();
                            if (isFoldingStation) {
                                foldingComplete = true;
                            }
                        }
                    }
                    if (te instanceof IOStationStatusUpdateEvent) {
                        if (((IOStationStatusUpdateEvent) te).getStatus().equals(HandshakeCapability.ServerSideStates.COMPLETE)) {
                            Optional.ofNullable(knownActors.get(((IOStationStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((IOStationStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                        }
                    }
                    if (te instanceof ReadyForProcessEvent) {
                        //assert((ReadyForProcessEvent) te).isReady();
                        // We don't know the machine Id here so we just tell every folding station
                        if (!isProcessAssigned) {
                            knownActors.values().stream()
                                    .filter(m -> m.getId().toLowerCase(Locale.ROOT).contains("Folding".toLowerCase(Locale.ROOT)))
                                    .forEach(actor -> actor.getAkkaActor().tell(new LockForOrder(step.getID(), "Test"), getRef()));
                            isProcessAssigned = true;
                        }
                    }
                }
                assertTrue(foldingComplete);

            }
        };
    }

    public Set<String> getLocalhostLayout() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://localhost:4840/milo"); //Pos34 input station (West of TT)
        urlsToBrowse.add("opc.tcp://localhost:4842/milo"); // TT1 Pos20
        urlsToBrowse.add("opc.tcp://localhost:4845/milo"); // Pos31 FoldingStation1 (North of TT)
        urlsToBrowse.add("opc.tcp://localhost:4847/milo"); // Pos37 FoldingStation2 (South of TT)
        urlsToBrowse.add("opc.tcp://localhost:4850/milo"); // Pos23 OutputStation (East of TT)
        return urlsToBrowse;
    }

    public Set<String> getTestLayout() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://127.0.0.1:4847/milo"); //Input West of TT
        urlsToBrowse.add("opc.tcp://127.0.0.1:4848/milo"); //InternalTT

        urlsToBrowse.add("opc.tcp://127.0.0.1:4849/milo"); //Folding1
        urlsToBrowse.add("opc.tcp://127.0.0.1:4850/milo"); //Folding2
        urlsToBrowse.add("opc.tcp://127.0.0.1:4851/milo"); //Folding3

        urlsToBrowse.add("opc.tcp://127.0.0.1:4852/milo"); //TransitStation
        return urlsToBrowse;
    }

    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }
}
