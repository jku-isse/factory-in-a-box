package fiab.mes.frontend;

import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.ProcessStep;
import ProcessCore.impl.ProcessImpl;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.http.javadsl.ServerBinding;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.ShopfloorStartup;
import fiab.mes.capabilities.plotting.EcoreProcessUtils;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.OrderRelocationNotification;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.ecore.ProduceFoldingProcess;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.productioncell.FoldingProductionCell;
import fiab.mes.productioncell.foldingstation.FoldingProductionCellCoordinator;
import fiab.mes.transport.actor.transportsystem.FoldingTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.HardcodedFoldingTransportRoutingAndMapping;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.opcua.CapabilityImplementationMetadata;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestComposedFoldingStationOPCUA {

    private static ActorSystem system;
    private static String ROOT_SYSTEM = "routes";
    private static ActorSelection machineEventBus;
    private static ActorSelection orderEventBus;
    private static ActorSelection orderEntryActor;
    private static CompletionStage<ServerBinding> binding;
    private static CompletionStage<ServerBinding> cellBinding;

    private static final Logger logger = LoggerFactory.getLogger(OrderEmittingTestServerWithOPCUA.class);
    static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownFoldingActors = new HashMap<>();

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        system = ActorSystem.create(ROOT_SYSTEM);

        binding = ShopfloorStartup.startupFolding(null, 3, system);
        cellBinding = FoldingProductionCell.startup("LocalFoldingProductionCell", 1, ActorSystem.create("FoldingProductionCellCoordinator"));

        orderEventBus = system.actorSelection("/user/" + OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        machineEventBus = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        orderEntryActor = system.actorSelection("/user/" + OrderEntryActor.WELLKNOWN_LOOKUP_NAME);//.resolveOne(Timeout.create(Duration.ofSeconds(3)))..;

    }

    @BeforeEach
    void setUp() {
        knownFoldingActors = new HashMap<>();

    }

    @AfterAll
    public static void teardown() {
        binding.thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> {
                    TestKit.shutdownActorSystem(system);
                }); // and shutdown when done
        cellBinding.thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> {
                    TestKit.shutdownActorSystem(system);
                }); // and shutdown when done
        system.terminate();
        system = null;
    }

    @Test
    void testShopfloorFoldingProcess(){
        //Don't forget to manually reset hidden output and load localIO and transitStation
        OrderProcess op = new OrderProcess(ProduceFoldingProcess.getSequentialBoxProcess("Test"));
        RegisterProcessRequest processRequest = new RegisterProcessRequest("Test", op, ActorRef.noSender());
        testShopfloorProcess(processRequest);
    }

    @Test
    void testShopfloorPlottingProcess(){
        OrderProcess op = new OrderProcess(ProduceProcess.getSingleBlackStepProcess("Test"));
        RegisterProcessRequest processRequest = new RegisterProcessRequest("Test", op,  ActorRef.noSender());
        testShopfloorProcess(processRequest);
    }

    @Test
    void testShopfloorPlotAndFoldingProcess(){
        //Don't forget to manually reset hidden output and load localIO and transitStation
        OrderProcess op = new OrderProcess(ProduceFoldingProcess.getSequentialDrawAndFoldBoxProcess("Test"));
        RegisterProcessRequest processRequest = new RegisterProcessRequest("Test", op,  ActorRef.noSender());
        testShopfloorProcess(processRequest);
    }

    void testShopfloorProcess(RegisterProcessRequest processRequest) {
        new TestKit(system) {
            {
                System.out.println("test frontend responses by emitting orders with sequential process");

                orderEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")), getRef());
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")), getRef());

                Set<String> urlsToBrowse = getLocalhostLayout();
                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<>();
                ShopfloorConfigurations.addSpawners(capURI2Spawning, new FoldingTransportPositionLookup(), new HardcodedFoldingTransportRoutingAndMapping());
                urlsToBrowse.forEach(url -> {
                    ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
                    try {
                        discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                //OrderProcess op = new OrderProcess(ProduceFoldingProcess.getSequentialBoxProcess("Test"));
                //RegisterProcessRequest processRequest = new RegisterProcessRequest("Test", op, getRef());

                int countConnEvents = 0;
                boolean isPlannerFunctional = false;
                boolean isTransport1Functional = false;
                int idleEvents = 0;

                while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() ||
                        !isTransport1Functional || idleEvents < urlsToBrowse.size()) {
                    ignoreMsg(msg -> msg instanceof String);
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), TimedEvent.class);
                    logEvent(te);
                    if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlanerStatusMessage.PlannerState.FULLY_OPERATIONAL)) {
                        isPlannerFunctional = true;
                    }
                    if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState().equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
                        if (!isTransport1Functional) {
                            isTransport1Functional = true;
                        }
                    }
                    if (te instanceof MachineConnectedEvent) {
                        countConnEvents++;
                        knownFoldingActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof IOStationStatusUpdateEvent) {
                        if (((IOStationStatusUpdateEvent) te).getStatus().equals(HandshakeCapability.ServerSideStates.STOPPED))
                            Optional.ofNullable(knownFoldingActors.get(((IOStationStatusUpdateEvent) te).getMachineId()))
                                    .ifPresent(actor -> actor.getAkkaActor()
                                            .tell(new GenericMachineRequests.Reset(((IOStationStatusUpdateEvent) te).getMachineId()), getRef())
                                    );
                        if (((IOStationStatusUpdateEvent) te).getStatus().equals(HandshakeCapability.ServerSideStates.IDLE_EMPTY) ||
                                ((IOStationStatusUpdateEvent) te).getStatus().equals(HandshakeCapability.ServerSideStates.IDLE_LOADED)) {
                            idleEvents++;
                        }
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
                            Optional.ofNullable(knownFoldingActors.get(((MachineStatusUpdateEvent) te).getMachineId()))
                                    .ifPresent(actor -> actor.getAkkaActor()
                                            .tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                                    );
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.IDLE)) {
                            idleEvents++;
                        }
                    }
                }
                orderEntryActor.tell(processRequest, getRef());

                boolean reachedOutput = false;

                while (!reachedOutput) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), TimedEvent.class);
                    logEvent(te);
                    if (te instanceof IOStationStatusUpdateEvent) {
                        if (((IOStationStatusUpdateEvent) te).getStatus().equals(HandshakeCapability.ServerSideStates.IDLE_EMPTY) ||
                                ((IOStationStatusUpdateEvent) te).getStatus().equals(HandshakeCapability.ServerSideStates.IDLE_LOADED)) {
                            //If event comes from an outputStation we can assume here the pallet reached the final out
                            reachedOutput = Optional.ofNullable(knownFoldingActors.get(((IOStationStatusUpdateEvent) te).getMachineId()))
                                    .filter(m -> m.getId().toLowerCase().contains("Output".toLowerCase())).isPresent();
                        }
                    }
                }
                int totalUrlsToBrowse = urlsToBrowse.size();
                assertEquals(totalUrlsToBrowse, knownFoldingActors.size());
                assertTrue(reachedOutput);
            }
        };
    }

    public Set<String> getLocalhostLayout() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://127.0.0.1:4840/milo"); //Input
        urlsToBrowse.add("opc.tcp://127.0.0.1:4841/milo"); //Plot
        urlsToBrowse.add("opc.tcp://127.0.0.1:4842/milo"); //Output
        urlsToBrowse.add("opc.tcp://127.0.0.1:4843/milo"); //TT1
        urlsToBrowse.add("opc.tcp://127.0.0.1:4844/milo"); //TT2
        urlsToBrowse.add("opc.tcp://127.0.0.1:4845/milo"); //Plot
        urlsToBrowse.add("opc.tcp://127.0.0.1:4846/milo"); //Plot

        urlsToBrowse.add("opc.tcp://127.0.0.1:4849/milo"); //Fold
        urlsToBrowse.add("opc.tcp://127.0.0.1:4850/milo"); //Fold
        urlsToBrowse.add("opc.tcp://127.0.0.1:4851/milo"); //Fold
        urlsToBrowse.add("opc.tcp://127.0.0.1:4852/milo"); //Transit
        urlsToBrowse.add("opc.tcp://127.0.0.1:4853/milo"); //TT3
        urlsToBrowse.add("opc.tcp://127.0.0.1:4854/milo"); //Output
        return urlsToBrowse;
    }

    public Set<String> getRealLayout() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Input
        urlsToBrowse.add("opc.tcp://192.168.0.31:4840"); //Plot
        urlsToBrowse.add("opc.tcp://192.168.0.35:4840"); //Output
        urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo"); //TT1
        urlsToBrowse.add("opc.tcp://192.168.0.21:4842/milo"); //TT2
        urlsToBrowse.add("opc.tcp://192.168.0.37:4840"); //Plot
        urlsToBrowse.add("opc.tcp://192.168.0.38:4840"); //Plot

        urlsToBrowse.add("opc.tcp://192.168.0.24:4849/milo"); //Fold
        urlsToBrowse.add("opc.tcp://192.168.0.24:4850/milo"); //Fold
        urlsToBrowse.add("opc.tcp://192.168.0.41:4852"); //Transit
        urlsToBrowse.add("opc.tcp://192.168.0.40:4853"); //TT3
        urlsToBrowse.add("opc.tcp://192.168.0.40:4854"); //Output
        return urlsToBrowse;
    }

    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }
}
