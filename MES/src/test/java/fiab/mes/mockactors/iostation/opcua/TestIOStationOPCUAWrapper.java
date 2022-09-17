package fiab.mes.mockactors.iostation.opcua;

import java.time.Duration;

import akka.actor.PoisonPill;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.iostation.InputStationFactory;
import fiab.opcua.client.FiabOpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.IOStationCapability;
//import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.machine.actor.iostation.wrapper.IOStationOPCUAWrapper;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.mockactors.iostation.VirtualIOStationActorFactory;
import fiab.opcua.client.OPCUAClientFactory;
import testutils.PortUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
class TestIOStationOPCUAWrapper {

    private static final Logger logger = LoggerFactory.getLogger(TestIOStationOPCUAWrapper.class);

    MachineEventBus intraEventBus;
    AbstractCapability capability;
    Actor model;

    IOStationOPCUAWrapper wrapper;
    ActorRef machineProxy;
    ActorRef remoteMachine;
    ActorRef machineEventBusWrapper;
    ActorSystem system;

    FiabOpcUaClient client;

    /*public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("TEST_ROOT_SYSTEM");
        InputStationFactory.startStandaloneInputStation(system, 4840, "InputStation");
    }*/

    /*@BeforeAll
    static void init() {
        //inputStation = InputStationFactory.startStandaloneInputStation(ActorSystem.create("VirtualRemote"), 4840, "InputStation");
    }*/

    @BeforeEach
    void setup() throws Exception {
        NodeId capabilitImpl = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU");
        NodeId resetMethod = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/RESET");
        NodeId stopMethod = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/STOP");
        NodeId stateVar = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/STATE");
        system = ActorSystem.create("TEST_ROOT_SYSTEM");
        boolean isInputStation = true;
        capability = isInputStation ? IOStationCapability.getInputStationCapability() : IOStationCapability.getOutputStationCapability();
        int remoteIp = PortUtils.findNextFreePort();
        remoteMachine = InputStationFactory.startStandaloneInputStation(system, remoteIp, "InputStation");
        // assume OPCUA server (mock or otherwise is started
        intraEventBus = new MachineEventBus();
        client = OPCUAClientFactory.createFIABClientAndConnect("opc.tcp://127.0.0.1:" + remoteIp);
        machineEventBusWrapper = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        wrapper = new IOStationOPCUAWrapper(intraEventBus, client, capabilitImpl, stopMethod, resetMethod, stateVar, null);
        model = VirtualIOStationActorFactory.getDefaultIOStationActor(isInputStation, 34);
    }

    @AfterEach
    void teardown() {
        client.disconnectClient();
        TestKit.shutdownActorSystem(system);
    }

    /*@Test
    void testReset() {
        wrapper.reset();
    }*/

    @Test
    void testSubscribeState() throws InterruptedException {
        assertDoesNotThrow(() -> {
            wrapper.subscribeToStatus();
        });
        //wrapper.subscribeToStatus();
        //Thread.sleep(10000);
    }

    @Test
    void testMachineConnectedEvent() {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                machineProxy = system.actorOf(BasicIOStationActor.props(eventBusByRef, capability, model, wrapper, intraEventBus), model.getActorName());
                expectMsgClass(Duration.ofSeconds(10), MachineConnectedEvent.class);
            }
        };
    }

    @Test
    void testActorIntegration() {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());
                //Create the proxy and wait for connected event
                machineProxy = system.actorOf(BasicIOStationActor.props(eventBusByRef, capability, model, wrapper, intraEventBus), model.getActorName());
                expectMsgClass(Duration.ofSeconds(10), MachineConnectedEvent.class);
                //Check whether the status gets forwarded
                IOStationStatusUpdateEvent ioStationStatusUpdateEvent = expectMsgClass(IOStationStatusUpdateEvent.class);
                assertEquals(ServerSideStates.STOPPED, ioStationStatusUpdateEvent.getStatus());
                //Reset and wait for idle loaded status to know we are done
                machineProxy.tell(new GenericMachineRequests.Reset(ioStationStatusUpdateEvent.getMachineId()), getRef());
                ioStationStatusUpdateEvent = expectMsgClass(IOStationStatusUpdateEvent.class);
                assertEquals(ServerSideStates.RESETTING, ioStationStatusUpdateEvent.getStatus());

                ioStationStatusUpdateEvent = expectMsgClass(IOStationStatusUpdateEvent.class);
                assertEquals(ServerSideStates.IDLE_LOADED, ioStationStatusUpdateEvent.getStatus());
            }
        };
    }

}
