package fiab.mes.proxy.turntable.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.proxy.ioStation.inputStation.testutils.InputStationPositionParser;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.shopfloor.DefaultTestLayout;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.shopfloor.utils.ShopfloorUtils;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.TurntableFactory;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TestTurntableProxyIntegration {

    private ActorSystem system;
    private ActorRef machineEventBus;
    private OPCUABase opcuaBase;

    @BeforeEach
    public void setup() {
        opcuaBase = OPCUABase.createAndStartLocalServer(4840, "VirtualTurntable");
        system = ActorSystem.create("TestSystem");
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        opcuaBase.shutDownOpcUaBase();
    }

    @Test
    public void testResetAndStopTurntableOpcUaStateChange() {
        new TestKit(system) {
            {
                TurntableFactory.startStandaloneTurntable(system,opcuaBase);
                String remoteEndpoint = "opc.tcp://127.0.0.1:4840";
                //Start listening to machine events
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), machineEventBus, new InputStationPositionParser());
                discoveryUtil.discoverCapabilityForEndpoint(remoteEndpoint);

                MachineConnectedEvent event = expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected
                ActorRef turntableProxy = event.getMachine().getAkkaActor();

                expectMachineStatusUpdate(this, BasicMachineStates.STOPPED);
                //Automatic reset from proxy is called here
                expectMachineStatusUpdate(this, BasicMachineStates.RESETTING);
                expectMachineStatusUpdate(this, BasicMachineStates.IDLE);

                try {
                    FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect(remoteEndpoint);
                    NodeId statusNode = NodeId.parse("ns=2;s=VirtualTurntable/STATE");
                    String currentState = client.readStringVariableNode(statusNode);
                    assertEquals(BasicMachineStates.IDLE.name(), currentState);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                turntableProxy.tell(new GenericMachineRequests.Stop(getRef().path().name()), ActorRef.noSender());
                expectMachineStatusUpdate(this, BasicMachineStates.STOPPING);
                expectMachineStatusUpdate(this, BasicMachineStates.STOPPED);
            }
        };
    }

    @Test
    public void testActorProxyFromTestLayout(){
        new TestKit(system){
            {
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());
                DefaultTestLayout layout = new DefaultTestLayout(system, machineEventBus);
                layout.initializeDefaultLayoutWithProxies();
                ActorRef turntable = layout.getMachineById(ShopfloorUtils.TURNTABLE_1);
                ActorRef ttProxy = layout.getMachineProxyById(ShopfloorUtils.TURNTABLE_1);

                ttProxy.tell(new GenericMachineRequests.Reset("Tester"), getRef());

                fishForMessage(Duration.ofSeconds(10), "Turntable successfully reset", msg ->
                        msg instanceof MachineStatusUpdateEvent &&
                        ((MachineStatusUpdateEvent)msg).getStatus() == BasicMachineStates.IDLE);
                //FIXME remote tt does not reach resetting!!!
            }
        };
    }

    private void expectMachineStatusUpdate(TestKit probe, BasicMachineStates state){
        MachineStatusUpdateEvent event = probe.expectMsgClass(Duration.ofSeconds(10), MachineStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }
}
