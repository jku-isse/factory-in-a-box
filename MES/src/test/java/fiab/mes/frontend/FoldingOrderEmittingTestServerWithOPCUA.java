package fiab.mes.frontend;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fiab.mes.order.ecore.ProduceFoldingProcess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.http.javadsl.ServerBinding;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.mes.DefaultShopfloorInfrastructure;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.ShopfloorStartup;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.mockactors.transport.opcua.TestTurntableWithIOStations;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;

public class FoldingOrderEmittingTestServerWithOPCUA {

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
    void testFrontendResponsesByEmittingOrdersSequentialProcess() throws Exception {
        new TestKit(system) {
            {
                System.out.println("test frontend responses by emitting orders with sequential process");

                orderEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")), getRef());
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")), getRef());

                Set<String> urlsToBrowse = getLocalhostLayout();
                //Set<String> urlsToBrowse = getSingleTTLayout(); //set layout to 1 expectedTT in preTEst method
                Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
                ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);

                urlsToBrowse.stream().forEach(url -> {
                    ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
                    try {
                        discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
                        TimeUnit.SECONDS.sleep(2);
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
                    if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
                        isPlannerFunctional = true;
                    }
                    if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState().equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
                        isTransportFunctional = true;
                    }
                    if (te instanceof MachineConnectedEvent) {
                        countConnEvents++;
                        knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    // DO THIS MANUALLY FROM WEB UI!
                    if (te instanceof MachineStatusUpdateEvent) {
                        if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
                            Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId()))
                                    .ifPresent(actor -> actor.getAkkaActor()
                                            .tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
                            );
                    }
                }

                CountDownLatch count = new CountDownLatch(1);
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
                System.out.println("Test completed by keyboard input " + i);
            }
        };

    }

    public Set<String> getLocalhostLayout() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://localhost:4840/milo"); //Pos34 input station (West of TT)
        urlsToBrowse.add("opc.tcp://localhost:4841/milo"); // Pos35 Output
        urlsToBrowse.add("opc.tcp://localhost:4845/milo"); // Pos31 FoldingStation1 (North of TT)
        urlsToBrowse.add("opc.tcp://localhost:4842/milo"); // TT1 Pos20
        urlsToBrowse.add("opc.tcp://localhost:4843/milo"); // TT2 Pos21
        //urlsToBrowse.add("opc.tcp://localhost:4847/milo"); // Pos37 BufferStation (South of TT)
        return urlsToBrowse;
    }

    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }
}