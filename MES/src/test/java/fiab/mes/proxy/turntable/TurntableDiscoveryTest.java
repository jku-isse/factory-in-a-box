package fiab.mes.proxy.turntable;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.proxy.turntable.testutils.TurntableStationPositionParser;
import fiab.mes.shopfloor.layout.DefaultTestLayout;
import fiab.mes.shopfloor.utils.ShopfloorUtils;
import org.junit.jupiter.api.*;

import java.util.Set;

@Tag("IntegrationTest")
public class TurntableDiscoveryTest {

    private ActorSystem system;
    private ActorRef machineEventBus;
    //private OPCUABase opcuaBase;
    private DefaultTestLayout layout;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("TestSystem");
        //opcuaBase = OPCUABase.createAndStartLocalServer(4840, "VirtualTurntable");
        //TurntableFactory.startStandaloneTurntable(system,opcuaBase);
        //TurntableFactory.startStandaloneTurntable(system, 4840, "VirtualTurntable");
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        layout = new DefaultTestLayout(system, machineEventBus);

    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        //opcuaBase.shutDownOpcUaBase();
    }

    @Test
    public void testTurntableDiscovery() {
        new TestKit(system) {
            {
                //Start listening to machine events
                String turntableId = ShopfloorUtils.TURNTABLE_1;
                //machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());
                layout.subscribeToInterMachineEventBus(getRef(), getRef().path().name());
                layout.initializeParticipantsForId(Set.of(turntableId));
                String turntableEndpoint = layout.getMachineEndpoint(turntableId);

                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), new TurntableStationPositionParser());

                discoveryUtil.discoverCapabilityForEndpoint(turntableEndpoint);

                expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected
                expectMsgClass(MachineStatusUpdateEvent.class);   //Then we check whether we can receive status updates
            }
        };
    }

}