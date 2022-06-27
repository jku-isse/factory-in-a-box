package fiab.mes.proxy.turntable;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.proxy.turntable.testutils.TurntableStationPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.TurntableFactory;
import org.junit.jupiter.api.*;

@Tag("IntegrationTest")
public class TurntableDiscoveryTest {

    private ActorSystem system;
    private ActorRef machineEventBus;
    private OPCUABase opcuaBase;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("TestSystem");
        opcuaBase = OPCUABase.createAndStartLocalServer(4840, "VirtualTurntable");
        TurntableFactory.startStandaloneTurntable(system,opcuaBase);
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        opcuaBase.shutDownOpcUaBase();
    }

    @Test
    public void testTurntableDiscovery() {
        new TestKit(system) {
            {
                //Start listening to machine events
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), machineEventBus, new TurntableStationPositionParser());

                discoveryUtil.discoverCapabilityForEndpoint("opc.tcp://127.0.0.1:4840");

                expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected
                expectMsgClass(MachineStatusUpdateEvent.class);   //Then we check whether we can receive status updates
            }
        };
    }

}