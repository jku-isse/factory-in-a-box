package fiab.mes.proxy.ioStation.outputStation;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import fiab.iostation.OutputStationFactory;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.proxy.ioStation.outputStation.testutils.OutputStationPositionParser;
import fiab.opcua.server.OPCUABase;
import org.junit.jupiter.api.*;

@Tag("IntegrationTest")
public class OutputStationDiscoveryTest {

    private ActorSystem system;
    private ActorRef machineEventBus;
    private OPCUABase opcuaBase;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("TestSystem");
        opcuaBase = OPCUABase.createAndStartLocalServer(4840, "VirtualOutputStation");
        OutputStationFactory.startStandaloneOutputStation(system, opcuaBase);
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        opcuaBase.shutDownOpcUaBase();
    }

    @Test
    public void testOutputStationDiscovery() {
        new TestKit(system) {
            {
                //Start listening to machine events
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), new OutputStationPositionParser());

                discoveryUtil.discoverCapabilityForEndpoint("opc.tcp://127.0.0.1:4840");

                expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected
                expectMsgClass(IOStationStatusUpdateEvent.class);   //Then we check whether we can receive status updates
            }
        };
    }

}