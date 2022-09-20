package fiab.mes.proxy.turntable;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.proxy.ioStation.inputStation.testutils.InputStationPositionParser;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.proxy.turntable.testutils.TurntableStationPositionParser;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.TurntableFactory;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class TurntableProxyTest {

    private ActorSystem system;
    private ActorRef machineEventBus;
    private OPCUABase opcuaBase;

    @BeforeEach
    public void setup() {
        opcuaBase = OPCUABase.createAndStartLocalServer(4840, "VirtualTurntable");
        system = ActorSystem.create("TestSystem");
        TurntableFactory.startStandaloneTurntable(system,opcuaBase);
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        opcuaBase.shutDownOpcUaBase();
    }

    @Test
    public void testResetAndStopTurntable() {
        new TestKit(system) {
            {
                //Start listening to machine events
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), machineEventBus, new InputStationPositionParser());
                discoveryUtil.discoverCapabilityForEndpoint("opc.tcp://127.0.0.1:4840");

                MachineConnectedEvent event = expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected
                ActorRef turntable = event.getMachine().getAkkaActor();

                expectMachineStatusUpdate(this, BasicMachineStates.STOPPED);
                //Automatic reset from proxy is called here
                expectMachineStatusUpdate(this, BasicMachineStates.RESETTING);
                expectMachineStatusUpdate(this, BasicMachineStates.IDLE);

                turntable.tell(new GenericMachineRequests.Stop(getRef().path().name()), ActorRef.noSender());
                expectMachineStatusUpdate(this, BasicMachineStates.STOPPING);
                expectMachineStatusUpdate(this, BasicMachineStates.STOPPED);
            }
        };
    }

    private void expectMachineStatusUpdate(TestKit probe, BasicMachineStates state){
        MachineStatusUpdateEvent event = probe.expectMsgClass(Duration.ofSeconds(10), MachineStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }
}