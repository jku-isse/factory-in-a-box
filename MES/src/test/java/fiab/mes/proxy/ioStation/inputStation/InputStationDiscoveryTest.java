package fiab.mes.proxy.ioStation.inputStation;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import fiab.iostation.InputStationFactory;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.proxy.ioStation.inputStation.testutils.InputStationPositionParser;
import fiab.opcua.server.OPCUABase;
import org.junit.jupiter.api.*;
import testutils.PortUtils;


import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class InputStationDiscoveryTest {

    private ActorSystem system;
    private ActorRef machineEventBus;
    private OPCUABase opcuaBase;
    private int port;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("TestSystem");
        port = PortUtils.findNextFreePort();
        opcuaBase = OPCUABase.createAndStartLocalServer(port, "VirtualInputStation");
        InputStationFactory.startStandaloneInputStation(system, opcuaBase);
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        opcuaBase.shutDownOpcUaBase();
    }

    @Test
    public void testInputStationDiscovery() {
        new TestKit(system) {
            {
                //Start listening to machine events
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), new InputStationPositionParser());

                discoveryUtil.discoverCapabilityForEndpoint("opc.tcp://127.0.0.1:"+port);

                expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected
                expectMsgClass(IOStationStatusUpdateEvent.class);
            }
        };
    }
}