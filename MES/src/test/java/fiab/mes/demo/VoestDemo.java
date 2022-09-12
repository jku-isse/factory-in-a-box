package fiab.mes.demo;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.http.javadsl.ServerBinding;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.ShopfloorStartup;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.frontend.OrderEmittingTestServerWithOPCUA;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.mockactors.transport.opcua.TestTurntableWithIOStations;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.opcua.CapabilityImplementationMetadata;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

@Tag("AcceptanceTest")
public class VoestDemo {

    private static ActorSystem system;
    private static String ROOT_SYSTEM = "routes";
    private static ActorSelection machineEventBus;
    private static ActorSelection orderEventBus;
    private static ActorSelection orderEntryActor;
    private static CompletionStage<ServerBinding> binding;

    private static final Logger logger = LoggerFactory.getLogger(OrderEmittingTestServerWithOPCUA.class);
    static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
//	private static OrderProcess process;

    public static void main(String args[]) {
        // Dual TT tests:
        TestTurntableWithIOStations.startupW34toE35();
    }


    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        system = ActorSystem.create(ROOT_SYSTEM);

        binding = ShopfloorStartup.startup(null, 2, system);
        orderEventBus = system.actorSelection("/user/" + OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        machineEventBus = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        orderEntryActor = system.actorSelection("/user/" + OrderEntryActor.WELLKNOWN_LOOKUP_NAME);//.resolveOne(Timeout.create(Duration.ofSeconds(3)))..;

    }

    @BeforeEach
    void setUp() throws Exception {
    }

    @AfterAll
    public static void teardown() {
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> {
                    TestKit.shutdownActorSystem(system);
                }); // and shutdown when done
        system.terminate();
        system = null;
    }

    @Test
    void testTwoRedProcesses() throws Exception {
        List<RegisterProcessRequest> processRequests = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String oid = "P" + i + "-";
            OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleRedStepProcess(oid));
            RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, ActorRef.noSender());
            processRequests.add(req);
        }
        runDemoUsingProcesses(processRequests);
    }

    @Test
    void testTwoDifferentProcesses() throws Exception {
        List<RegisterProcessRequest> processRequests = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String oid = "P" + i + "-";
            OrderProcess op1;
            if (i % 2 == 0) {
                op1 = new OrderProcess(ProduceProcess.getSingleBlackStepProcess(oid));
            } else {
                op1 = new OrderProcess(ProduceProcess.getSequential4ColorProcess(oid));
            }
            RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, ActorRef.noSender());
            processRequests.add(req);
        }
        runDemoUsingProcesses(processRequests);
    }

    @Test
    void testMachineRelocation() throws Exception {
        List<RegisterProcessRequest> processRequests = new ArrayList<>();

        String oid = "P1-";
        OrderProcess op1 = new OrderProcess(ProduceProcess.getSequential4ColorProcess(oid));
        RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, ActorRef.noSender());
        processRequests.add(req);

        runDemoUsingProcesses(processRequests);
    }

    void runDemoUsingProcesses(List<RegisterProcessRequest> processRequests) throws Exception {
        new TestKit(system) {
            {
                System.out.println("test frontend responses by emitting orders with sequential process");

                orderEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")), getRef());
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")), getRef());

                Set<String> urlsToBrowse = getFullLayout();
                //Set<String> urlsToBrowse = getSingleTTLayout(); //set layout to 1 expectedTT in preTEst method
                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<>();
                ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);

                urlsToBrowse.forEach(url -> {
                    ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
                    discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
                });

                int countConnEvents = 0;
                boolean isPlannerFunctional = false;
                boolean isTransportFunctional = false;
                while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() - 1 || !isTransportFunctional) { // we expect one machine less, the spot we switch to, but which we nevertheless monitor
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), TimedEvent.class);
                    logEvent(te);
                    if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlanerStatusMessage.PlannerState.FULLY_OPERATIONAL)) {
                        isPlannerFunctional = true;
                    }
                    if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState().equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
                        isTransportFunctional = true;
                    }
                    if (te instanceof MachineConnectedEvent) {
                        //TODO only check for TTs, we can then relocate machines during a test without changing endpoints
                        countConnEvents++;
                        knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    // DO THIS MANUALLY FROM WEB UI!
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId())).ifPresent(
                                    actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                    }
                }
                int nProceesses = processRequests.size();
                CountDownLatch count = new CountDownLatch(nProceesses);
                while (count.getCount() > 0) {
                    RegisterProcessRequest req = processRequests.get(nProceesses - (int) count.getCount());
                    req.setRequestor(getRef());
                    orderEntryActor.tell(req, getRef());

                    count.countDown();
                    Thread.sleep(3000);
                }
                int palletsReachedOutput = 0;
                while (palletsReachedOutput < nProceesses) {
                    OrderEvent te = (OrderEvent) fishForMessage(Duration.ofSeconds(3600), "ignore MES messages",
                            m -> m instanceof OrderEvent);
                    logEvent(te);
                    if (te.getEventType().equals(OrderEvent.OrderEventType.REMOVED)) {
                        //Check for all pallets removed, either through premature removal or leaving output
                        palletsReachedOutput++;
                    }
                }
                Assertions.assertEquals(nProceesses, palletsReachedOutput);
            }
        };
    }

    public Set<String> getFullLayout() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34 west inputstation
        urlsToBrowse.add("opc.tcp://192.168.0.31:4840"); //Pos31 TT1 north plotter
        urlsToBrowse.add("opc.tcp://192.168.0.32:4840"); //Pos32 TT2 north plotter
        urlsToBrowse.add("opc.tcp://192.168.0.37:4840"); //Pos37 TT1 south plotter
        urlsToBrowse.add("opc.tcp://192.168.0.38:4840"); //Pos38 TT2 south plotter
        urlsToBrowse.add("opc.tcp://192.168.0.35:4840");    // POS EAST 35/ outputstation
        urlsToBrowse.add("opc.tcp://192.168.0.21:4842/milo");    // POS 21 TT2
        urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");        // Pos20 TT1
        return urlsToBrowse;
    }

    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }
}
