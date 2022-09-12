package fiab.mes.proxy.ioStation.inputStation;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.iostation.InputStationFactory;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.proxy.testutil.DiscoveryUtil;
import fiab.mes.proxy.ioStation.inputStation.testutils.InputStationPositionParser;
import fiab.opcua.server.OPCUABase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class InputStationProxyTest {

    private ActorSystem system;
    private ActorRef machineEventBus;
    private ActorRef actor;
    private OPCUABase opcuaBase;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("TestSystem");
        opcuaBase = OPCUABase.createAndStartLocalServer(4840, "VirtualInputStation");
        actor = InputStationFactory.startStandaloneInputStation(system, opcuaBase);
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @AfterEach
    public void teardown() {
        //From: https://doc.akka.io/docs/akka/current/testing.html
        //Remember to shut down the actor system after the test is finished (also in case of failure)
        // so that all actors—including the test actor—are stopped.
        TestKit.shutdownActorSystem(system);
        opcuaBase.shutDownOpcUaBase();
    }

    @Test
    public void testResetAndStopInputStation() {
        new TestKit(system) {
            {
                //Start listening to machine events
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), machineEventBus, new InputStationPositionParser());
                discoveryUtil.discoverCapabilityForEndpoint("opc.tcp://127.0.0.1:4840");

                expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected

                expectIOStatusUpdate(this, ServerSideStates.STOPPED);
                //Automatic reset from proxy is called here
                expectIOStatusUpdate(this, ServerSideStates.RESETTING);
                expectIOStatusUpdate(this, ServerSideStates.IDLE_LOADED);

                actor.tell(new StopRequest(getRef().path().name()), ActorRef.noSender());

                //expectIOStatusUpdate(this, ServerSideStates.STOPPING); proxy ignores stopping
                expectIOStatusUpdate(this, ServerSideStates.STOPPED);
            }
        };
    }

    @Test
    public void testInputStationFull() {
        new TestKit(system) {
            {
                //Start listening to machine events
                machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());
                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), machineEventBus, new InputStationPositionParser());
                discoveryUtil.discoverCapabilityForEndpoint("opc.tcp://127.0.0.1:4840");

                expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected

                expectIOStatusUpdate(this, ServerSideStates.STOPPED);
                expectIOStatusUpdate(this, ServerSideStates.RESETTING);
                expectIOStatusUpdate(this, ServerSideStates.IDLE_LOADED);

                actor.tell(new InitiateHandoverRequest(getRef().path().name()), getRef());
                //expectIOStatusUpdate(this, ServerSideStates.PREPARING);      //Seems to be skipped by proxy
                //expectIOStatusUpdate(this, ServerSideStates.STARTING);       //Seems to be skipped by proxy
                expectIOStatusUpdate(this, ServerSideStates.READY_LOADED);

                actor.tell(new StartHandoverRequest(getRef().path().name()), getRef());
                expectIOStatusUpdate(this, ServerSideStates.EXECUTE);
                //expectIOStatusUpdate(this, ServerSideStates.COMPLETING);     //Seems to be skipped by proxy
                expectIOStatusUpdate(this, ServerSideStates.COMPLETE);
            }
        };
    }

    private void expectIOStatusUpdate(TestKit probe, ServerSideStates state) {
        IOStationStatusUpdateEvent event = probe.expectMsgClass(Duration.ofSeconds(10),IOStationStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }
}