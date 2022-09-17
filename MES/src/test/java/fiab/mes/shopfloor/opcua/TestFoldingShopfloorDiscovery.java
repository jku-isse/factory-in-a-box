package fiab.mes.shopfloor.opcua;

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
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.ecore.ProduceFoldingProcess;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.opcua.CapabilityImplementationMetadata;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFoldingShopfloorDiscovery {

    private static ActorSystem system;
    private static String ROOT_SYSTEM = "routes";
    private static ActorSelection machineEventBus;
    private static ActorSelection orderEventBus;
    private static ActorSelection orderEntryActor;
    private static CompletionStage<ServerBinding> binding;

    private static final Logger logger = LoggerFactory.getLogger(OrderEmittingTestServerWithOPCUA.class);
    static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
//	private static OrderProcess process;

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
    @Tag("IntegrationTest") //FIXME boot up virtual machines before test
    void testVirtualShopfloorParticipantDiscovery() {
        Set<String> urlsToBrowse = getLocalhostLayout();
        discoverShopfloorParticipants(urlsToBrowse);
    }

    @Test
    @Tag("SystemTest")
    void testShopfloorParticipantDiscovery() {
        Set<String> urlsToBrowse = getRealLayout();
        discoverShopfloorParticipants(urlsToBrowse);
    }

    private void discoverShopfloorParticipants(Set<String> urlsToBrowse) {
        new TestKit(system) {
            {
                System.out.println("test frontend responses by emitting orders with sequential process");

                orderEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")), getRef());
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")), getRef());

                //Set<String> urlsToBrowse = getLocalhostLayout();
                //Set<String> urlsToBrowse = getRealLayout();
                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<>();
                ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);

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
                boolean isPlannerFunctional = false;
                boolean isTransportFunctional = false;
                while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() || !isTransportFunctional) {
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

                    /*if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId()))
                                    .ifPresent(actor -> actor.getAkkaActor()
                                            .tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                                    );
                    }*/
                }
                assertEquals(urlsToBrowse.size(), knownActors.size());

                /*CountDownLatch count = new CountDownLatch(1);
                while (count.getCount() > 0) {
                    String oid = "P" + String.valueOf(count.getCount() + "-");
                    OrderProcess op1 = new OrderProcess(ProduceFoldingProcess.getSequentialBoxProcess(oid));
                    RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, getRef());
                    orderEntryActor.tell(req, getRef());

                    count.countDown();
                    Thread.sleep(3000);
                }
                System.out.println("Finished with emitting orders. Press ENTER to end test!");
                int i = -1;
                while(i < 0){
                    i = System.in.read();
                }
                System.out.println("Test completed by keyboard input " + i);*/
            }
        };

    }

    public Set<String> getLocalhostLayout() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://localhost:4840/milo"); //Input
        urlsToBrowse.add("opc.tcp://localhost:4841/milo"); //Plot
        urlsToBrowse.add("opc.tcp://localhost:4842/milo"); //Output
        urlsToBrowse.add("opc.tcp://localhost:4843/milo"); //TT1
        urlsToBrowse.add("opc.tcp://localhost:4844/milo"); //TT2
        urlsToBrowse.add("opc.tcp://localhost:4845/milo"); //Plot
        urlsToBrowse.add("opc.tcp://localhost:4846/milo"); //Plot

        urlsToBrowse.add("opc.tcp://localhost:4849/milo"); //Fold
        urlsToBrowse.add("opc.tcp://localhost:4850/milo"); //Fold
        urlsToBrowse.add("opc.tcp://localhost:4851/milo"); //Fold
        urlsToBrowse.add("opc.tcp://localhost:4852/milo"); //Transit
        urlsToBrowse.add("opc.tcp://localhost:4853/milo"); //TT3
        urlsToBrowse.add("opc.tcp://localhost:4854/milo"); //Output
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
