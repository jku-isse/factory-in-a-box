package fiab.mes.proxy.roboticArm;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.roboticArm.RoboticArmCapability;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.assembly.order.actor.BikeAssemblyOrderEntryActor;
import fiab.mes.assembly.order.actor.BikeAssemblyOrderPlanningActor;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.machine.actor.roboticArm.RoboticArmProxy;
import fiab.mes.machine.actor.roboticArm.wrapper.RoboticArmOpcUaWrapper;
import fiab.mes.machine.actor.roboticArm.wrapper.RoboticArmWrapperInterface;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.proxy.roboticArm.utils.MockRoboticArm;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import testutils.PortUtils;

import java.time.Duration;

import static fiab.mes.shopfloor.participants.ParticipantInfo.localhostOpcUaPrefix;

public class TestRoboticArmProxy {

    ActorSystem system;
    MachineEventBus intraMachineEventBus;
    MockRoboticArm mockRoboticArm;
    String endpoint;

    public static void main(String[] args) {
        new MockRoboticArm(4840);
    }

    @BeforeEach
    public void setup() {
        int port = PortUtils.findNextFreePort();
        endpoint = "opc.tcp://127.0.0.1:" + port;
        mockRoboticArm = new MockRoboticArm(port);
        system = ActorSystem.create("TestRoboticArmProxy");
        intraMachineEventBus = new MachineEventBus();
        system.actorOf(InterMachineEventBusWrapperActor.propsWithPreparedBus(intraMachineEventBus), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        system.actorOf(BikeAssemblyOrderPlanningActor.props());
    }

    @AfterEach
    public void teardown() {
        mockRoboticArm.shutdown();
        TestKit.shutdownActorSystem(system);
    }

    @Test
    @Tag("SystemTest")
    public void testRemoteRoboticArmPickRequest() throws Exception {
        new TestKit(system){
            {
                String machineId = "Niryo";
                NodeId capabilityImplNode = NodeId.parse("ns=2;s=" + machineId + "/CAPABILITIES/CAPABILITY");
                NodeId resetMethod = NodeId.parse("ns=2;s=" + machineId + "/RESET");
                NodeId stopMethod = NodeId.parse("ns=2;s=" + machineId + "/STOP");
                NodeId pickMethod = NodeId.parse("ns=2;i=2");
                NodeId stateVar = NodeId.parse("ns=2;i=5");   //This will probably need to change too

                intraMachineEventBus.subscribe(getRef(), new MESSubscriptionClassifier("Tester", "*"));
                ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect("opc.tcp://10.78.115.145:4840");
                RoboticArmWrapperInterface hal = new RoboticArmOpcUaWrapper(intraMachineEventBus, client, capabilityImplNode, stopMethod, resetMethod, pickMethod, stateVar, null);
                ActorRef proxy = system.actorOf(RoboticArmProxy.props(eventBusByRef, prepareRoboticArmCapability(),
                        createParticipantModelActor("RoboticArm", 4840), hal, new MachineEventBus()));
                fishForMessage(Duration.ofSeconds(10), "Wait for status update", msg -> msg instanceof MachineStatusUpdateEvent);
                hal.pick("1");
                //fishForMessage(Duration.ofSeconds(10), "Wait for status update", msg -> msg instanceof MachineStatusUpdateEvent);
            }
        };
    }


    @Test
    @Tag("IntegrationTest")
    public void testRoboticArmDiscovery() throws Exception {
        new TestKit(system) {
            {
                String machineId = "Niryo";
                NodeId capabilityImplNode = NodeId.parse("ns=2;s=" + machineId + "/CAPABILITIES/CAPABILITY");
                NodeId resetMethod = NodeId.parse("ns=2;s=" + machineId + "/RESET");
                NodeId stopMethod = NodeId.parse("ns=2;s=" + machineId + "/STOP");
                NodeId pickMethod = NodeId.parse("ns=2;s=" + machineId + "/PickPart");
                NodeId stateVar = NodeId.parse("ns=2;s=" + machineId + "/STATE");

                intraMachineEventBus.subscribe(getRef(), new MESSubscriptionClassifier("Tester", "*"));
                ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect(endpoint);
                RoboticArmWrapperInterface hal = new RoboticArmOpcUaWrapper(intraMachineEventBus, client, capabilityImplNode, resetMethod, stopMethod, pickMethod, stateVar, null);
                ActorRef proxy = system.actorOf(RoboticArmProxy.props(eventBusByRef, prepareRoboticArmCapability(),
                        createParticipantModelActor("RoboticArm", 4840), hal, new MachineEventBus()));

                fishForMessage(Duration.ofSeconds(10), "Wait for Machine connected", msg -> msg instanceof MachineConnectedEvent);
            }
        };
    }

    @Test
    @Tag("IntegrationTest")
    public void testHardwareAbstractionLayer() throws Exception {
        new TestKit(system){
            {
                String machineId = "Niryo";
                NodeId capabilityImplNode = NodeId.parse("ns=2;s=" + machineId + "/CAPABILITIES/CAPABILITY");
                NodeId resetMethod = NodeId.parse("ns=2;s=" + machineId + "/RESET");
                NodeId stopMethod = NodeId.parse("ns=2;s=" + machineId + "/STOP");
                NodeId pickMethod = NodeId.parse("ns=2;s="+ machineId + "/PickPart");
                NodeId stateVar = NodeId.parse("ns=2;s=" + machineId + "/STATE");

                intraMachineEventBus.subscribe(getRef(), new MESSubscriptionClassifier("Tester", "*"));
                ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect(endpoint);
                RoboticArmWrapperInterface hal = new RoboticArmOpcUaWrapper(intraMachineEventBus, client, capabilityImplNode, stopMethod, resetMethod, pickMethod, stateVar, null);
                ActorRef proxy = system.actorOf(RoboticArmProxy.props(eventBusByRef, prepareRoboticArmCapability(),
                        createParticipantModelActor("RoboticArm", 4840), hal, new MachineEventBus()));
                fishForMessage(Duration.ofSeconds(10), "Wait for status update", msg -> msg instanceof MachineStatusUpdateEvent);
                hal.pick("test");
                fishForMessage(Duration.ofSeconds(10), "Wait for status update", msg -> msg instanceof MachineStatusUpdateEvent);
            }
        };
    }


    private AbstractCapability prepareRoboticArmCapability() {
        return RoboticArmCapability.getPickPartCapability();
    }


    private static Actor createParticipantModelActor(String machineId, int port) {
        Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
        actor.setID(machineId);
        actor.setActorName(machineId);
        actor.setDisplayName(machineId);
        actor.setUri(localhostOpcUaPrefix + port);
        return actor;
    }
}
