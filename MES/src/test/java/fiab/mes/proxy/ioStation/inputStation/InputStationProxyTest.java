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
import fiab.mes.shopfloor.layout.DefaultTestLayout;
import fiab.mes.shopfloor.utils.ShopfloorUtils;
import fiab.opcua.server.OPCUABase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import testutils.PortUtils;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("IntegrationTest")
public class InputStationProxyTest {

    private ActorSystem system;
    private ActorRef machineEventBus;
    private ActorRef actor;
    private OPCUABase opcuaBase;
    private String endpoint;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("TestSystem");
        int port = PortUtils.findNextFreePort();
        endpoint = "opc.tcp://127.0.0.1:" + port;
        opcuaBase = OPCUABase.createAndStartLocalServer(port, "VirtualInputStation");
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

                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), new InputStationPositionParser());
                discoveryUtil.discoverCapabilityForEndpoint(endpoint);

                expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected

                //expectIOStatusUpdate(this, ServerSideStates.STOPPED); //We are not guaranteed to receive this event for some reason!
                //Automatic reset from proxy is called here
                //expectIOStatusUpdate(this, ServerSideStates.RESETTING);
                //expectIOStatusUpdate(this, ServerSideStates.IDLE_LOADED);

                fishForMessage(Duration.ofSeconds(30), "Wait for resetting to finish", msg ->
                        msg instanceof IOStationStatusUpdateEvent &&
                                ((IOStationStatusUpdateEvent) msg).getStatus() == ServerSideStates.IDLE_LOADED);

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
                DiscoveryUtil discoveryUtil = new DiscoveryUtil(system, getRef(), new InputStationPositionParser());
                discoveryUtil.discoverCapabilityForEndpoint(endpoint);

                expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected

                fishForMessage(Duration.ofSeconds(30), "Wait for resetting to finish", msg ->
                        msg instanceof IOStationStatusUpdateEvent &&
                                ((IOStationStatusUpdateEvent) msg).getStatus() == ServerSideStates.IDLE_LOADED);

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

    @Test
    public void testLayoutProxyConfiguration(){
        new TestKit(system){
            {
                String inputStationId = ShopfloorUtils.INPUT_STATION;
                DefaultTestLayout layout = new DefaultTestLayout(system, machineEventBus);
                layout.subscribeToInterMachineEventBus(getRef(), getRef().path().name());
                layout.initializeParticipantsForId(Set.of(inputStationId));
                layout.runDiscovery(getRef());

                expectMsgClass(MachineConnectedEvent.class);        //First we get notified that we are connected
                IOStationStatusUpdateEvent statusUpdate = expectMsgClass(IOStationStatusUpdateEvent.class);
                assertEquals(ServerSideStates.STOPPED, statusUpdate.getStatus());

                ActorRef ttProxy = layout.getMachineById(inputStationId);   //FIXME use machine conn event instead to retrieve proxy

                ttProxy.tell(new GenericMachineRequests.Reset("Tester"), getRef());

                fishForMessage(Duration.ofSeconds(10), "InputStation successfully reset", msg ->
                        msg instanceof IOStationStatusUpdateEvent &&
                                ((IOStationStatusUpdateEvent) msg).getStatus() == ServerSideStates.IDLE_LOADED);

            }
        };
    }

    private void expectIOStatusUpdate(TestKit probe, ServerSideStates state) {
        IOStationStatusUpdateEvent event = probe.expectMsgClass(Duration.ofSeconds(10),IOStationStatusUpdateEvent.class);
        assertEquals(state, event.getStatus());
    }
}