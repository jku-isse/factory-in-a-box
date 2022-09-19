package fiab.mes.mockactors.productioncell;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
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
import fiab.mes.productioncell.FoldingProductionCell;
import fiab.mes.productioncell.foldingstation.DefaultFoldingCellTransportPositionLookup;
import fiab.mes.productioncell.foldingstation.FoldingProductionCellCoordinator;
import fiab.mes.productioncell.foldingstation.HardcodedFoldingCellTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.opcua.CapabilityImplementationMetadata;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestProductionCellDiscovery {

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
        HardcodedFoldingCellTransportRoutingAndMapping routing = new HardcodedFoldingCellTransportRoutingAndMapping();
        DefaultFoldingCellTransportPositionLookup dns = new DefaultFoldingCellTransportPositionLookup();
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), FoldingProductionCell.LOOKUP_PREFIX + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        transportCoord = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1, FoldingProductionCell.LOOKUP_PREFIX), FoldingProductionCell.LOOKUP_PREFIX + TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
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
    @Tag("IntegrationTest")
    void testVirtualProductionCellDiscovery() {
        Set<String> urlsToBrowse = getTestLayout();
        discoverProductionCell(urlsToBrowse);
    }

    @Test
    @Tag("SystemTest")
    void testProductionCellDiscovery(){
        Set<String> urlsToBrowse = getRealLayout();
        discoverProductionCell(urlsToBrowse);
    }

    void discoverProductionCell(Set<String> urlsToBrowse) {
        new TestKit(system) {
            {
                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
                ShopfloorConfigurations.addSpawners(capURI2Spawning, new DefaultFoldingCellTransportPositionLookup(),
                        new HardcodedFoldingCellTransportRoutingAndMapping());
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                urlsToBrowse.forEach(url -> {
                    ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
                    try {
                        discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

                int countConnEvents = 0;
                Set<String> respondingMachines = new HashSet<>();
                boolean isTransportFunctional = false;
                while (countConnEvents < urlsToBrowse.size() || !isTransportFunctional || respondingMachines.size() < urlsToBrowse.size()) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class);
                    logEvent(te);
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
                assertTrue(isTransportFunctional);
                assertEquals(countConnEvents, urlsToBrowse.size());
                assertEquals(respondingMachines.size(), urlsToBrowse.size());
            }
        };
    }

    public Set<String> getTestLayout() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://127.0.0.1:4847"); //Input West of TT
        urlsToBrowse.add("opc.tcp://127.0.0.1:4848"); //InternalTT

        urlsToBrowse.add("opc.tcp://127.0.0.1:4849"); //Folding1
        urlsToBrowse.add("opc.tcp://127.0.0.1:4850"); //Folding2
        urlsToBrowse.add("opc.tcp://127.0.0.1:4851"); //Folding3
        return urlsToBrowse;
    }

    public Set<String> getRealLayout() {
        Set<String> urlsToBrowse = new HashSet<>();
        urlsToBrowse.add("opc.tcp://192.168.0.24:4847/milo");
        urlsToBrowse.add("opc.tcp://192.168.0.41:4848");
        urlsToBrowse.add("opc.tcp://192.168.0.24:4849/milo");
        //urlsToBrowse.add("opc.tcp://192.168.0.24:4850/milo"); //We will keep it simple for now
        return urlsToBrowse;
    }

    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }
}
