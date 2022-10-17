package fiab.mes.proxy.turntable.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.shopfloor.layout.DefaultTestLayout;
import fiab.mes.shopfloor.utils.ShopfloorUtils;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestTurntableProxyIntegration {

    private ActorSystem system;
    private ActorRef interMachineEventBus;
    private String turntableId;
    private DefaultTestLayout layout;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("TestSystem");
        interMachineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        turntableId = ShopfloorUtils.TURNTABLE_1;
        layout = new DefaultTestLayout(system, interMachineEventBus);
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testResetAndStopTurntableOpcUaStateChange() {
        new TestKit(system) {
            {
                //Start listening to machine events
                layout.subscribeToInterMachineEventBus(getRef(), getRef().path().name());

                layout.initializeParticipantsForId(Set.of(turntableId));
                String remoteEndpoint = layout.getMachineEndpoint(turntableId);

                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), layout.getTransportPositionLookup());
                discoveryUtil.discoverCapabilityForEndpoint(remoteEndpoint);

                MachineConnectedEvent event = expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected
                ActorRef turntableProxy = event.getMachine().getAkkaActor();

                expectMachineStatusUpdate(this, BasicMachineStates.STOPPED);
                //Automatic reset from proxy is called here
                expectMachineStatusUpdate(this, BasicMachineStates.RESETTING);
                expectMachineStatusUpdate(this, BasicMachineStates.IDLE);

                assertDoesNotThrow(() -> {
                    FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect(remoteEndpoint);
                    NodeId statusNode = NodeId.parse("ns=2;s=" + turntableId + "/STATE");
                    String currentState = client.readStringVariableNode(statusNode);
                    assertEquals(BasicMachineStates.IDLE.name(), currentState);
                });

                turntableProxy.tell(new GenericMachineRequests.Stop(getRef().path().name()), ActorRef.noSender());
                expectMachineStatusUpdate(this, BasicMachineStates.STOPPING);
                expectMachineStatusUpdate(this, BasicMachineStates.STOPPED);
            }
        };
    }

    @Test
    public void testActorProxyFromTestLayout() {
        new TestKit(system) {
            {
                layout.subscribeToInterMachineEventBus(getRef(), getRef().path().name());
                layout.initializeParticipantsForId(Set.of(turntableId));
                layout.runDiscovery(getRef());
                MachineConnectedEvent event = expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected
                assertEquals(turntableId, event.getMachine().getModelActor().getActorName());
                //ActorRef turntable = layout.getMachineById(turntableId);
                ActorRef ttProxy = event.getMachine().getAkkaActor();

                ttProxy.tell(new GenericMachineRequests.Reset("Tester"), getRef());

                fishForMessage(Duration.ofSeconds(10), "Turntable successfully reset", msg ->
                        msg instanceof MachineStatusUpdateEvent &&
                                ((MachineStatusUpdateEvent) msg).getStatus() == BasicMachineStates.IDLE);
            }
        };
    }

    private void expectMachineStatusUpdate(TestKit probe, BasicMachineStates state) {
        MachineStatusUpdateEvent event = probe.expectMsgClass(Duration.ofSeconds(10), MachineStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }
}
