package fiab.mes.assembly.monitoring;

import ActorCoreModel.Actor;
import ActorCoreModel.ActorCoreModelPackage;
import ExtensionsCoreModel.ExtensionsCoreModelPackage;
import ExtensionsForAssemblyline.ExtensionsForAssemblylinePackage;
import InstanceExtensionModel.InstanceExtensionModelPackage;
import LinkedCoreModelActorToPart.LinkedCoreModelActorToPartPackage;
import PartCoreModel.PartCoreModelPackage;
import PriorityExtensionModel.PriorityExtensionModelPackage;
import ProcessCore.AbstractCapability;
import ProcessCore.Process;
import ProcessCore.ProcessStep;
import ProcessCore.XmlRoot;
import VariabilityExtensionModel.VariabilityExtensionModelPackage;
import actorprocess.ActorprocessPackage;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import at.pro2future.shopfloors.interfaces.impl.FileDataSource;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.roboticArm.RoboticArmCapability;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.assembly.monitoring.actor.AssemblyMonitoringActor;
import fiab.mes.assembly.monitoring.message.PartsPickedNotification;
import fiab.mes.assembly.order.message.ExtendedRegisterProcessRequest;
import fiab.mes.eventbus.*;
import fiab.mes.machine.actor.roboticArm.RoboticArmProxy;
import fiab.mes.machine.actor.roboticArm.wrapper.RoboticArmOpcUaWrapper;
import fiab.mes.machine.actor.roboticArm.wrapper.RoboticArmWrapperInterface;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.logger.KieRuntimeLogger;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import partprocess.PartprocessPackage;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static fiab.mes.shopfloor.participants.ParticipantInfo.localhostOpcUaPrefix;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMonitoringActor {

    private ActorSystem system;
    private ActorRef monitoringActor;
    private ActorRef monitorEventBus;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("MonitoringUnitTest");
        KieServices ks = KieServices.get();
        KieContainer kc = ks.getKieClasspathContainer();
        KieSession kieSession = kc.newKieSession("MonitoringKeySession");

        monitorEventBus = system.actorOf(AssemblyMonitoringEventBusWrapperActor.props(), AssemblyMonitoringEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        monitoringActor = system.actorOf(AssemblyMonitoringActor.props(OPCUABase.createAndStartLocalServer(4840, "Monitoring"), kieSession));
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    @Tag("AcceptanceTest")
    public void runMockIntegrationDemo() {
        new TestKit(system) {
            {
                MachineEventBus interMachineEventBus = new MachineEventBus();
                ActorRef interMachineEventBusWrapper = system.actorOf(InterMachineEventBusWrapperActor.propsWithPreparedBus(interMachineEventBus),
                        InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);

                String machineId = "Niryo";
                NodeId capabilityImplNode = NodeId.parse("ns=2;s=" + machineId + "/CAPABILITIES/CAPABILITY");
                NodeId resetMethod = NodeId.parse("ns=2;s=" + machineId + "/RESET");
                NodeId stopMethod = NodeId.parse("ns=2;s=" + machineId + "/STOP");
                NodeId pickMethod = NodeId.parse("ns=2;i=2");
                NodeId stateVar = NodeId.parse("ns=2;i=5");   //This will probably need to change too

                ActorRef orderEventBus = system.actorOf(AssemblyMonitoringActor.TestEventBusActor.props(), AssemblyMonitoringActor.TestEventBusActor.WRAPPER_ACTOR_LOOKUP_NAME);
                TestKit partPickReceiver = new TestKit(system);
                orderEventBus.tell(new SubscribeMessage(partPickReceiver.getRef(), new MESSubscriptionClassifier("PTester", "*")), partPickReceiver.getRef());
                interMachineEventBus.subscribe(getRef(), new MESSubscriptionClassifier("Tester", "*"));
                ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                FiabOpcUaClient client = null;
                while (client == null) {
                    try {
                        client = OPCUAClientFactory.createFIABClientAndConnect("opc.tcp://10.78.115.67:4840");
                        System.out.println("Found robotic arm, starting test");
                    } catch (Exception e) {
                        //e.printStackTrace();
                        System.out.println("Robotic arm not started, retrying ...");
                    }
                }
                RoboticArmWrapperInterface hal = new RoboticArmOpcUaWrapper(interMachineEventBus, client, capabilityImplNode, stopMethod, resetMethod, pickMethod, stateVar, null);
                ActorRef proxy = system.actorOf(RoboticArmProxy.props(eventBusByRef, prepareRoboticArmCapability(),
                        createParticipantModelActor("RoboticArm", 4840), hal, new MachineEventBus()));
                fishForMessage(Duration.ofSeconds(120), "Wait for status update", msg -> msg instanceof MachineStatusUpdateEvent);
                //hal.pick("1");
                System.out.println("Waiting for part to be picked after init sequence...");
                boolean done = false;
                while (!done) {
                    PartsPickedNotification partNotification = partPickReceiver.expectMsgClass(Duration.ofSeconds(360), PartsPickedNotification.class);
                    if (partNotification.getPartId().equals("wheel")) {
                        System.out.println("Recognized wheel, starting to pick parts ...");
                        hal.pick("2");
                    }else if (partNotification.getPartId().equals("mud_guard")){
                        System.out.println("Recognized mud guard, starting to pick parts ...");
                        hal.pick("1");
                    }else {
                        System.out.println("Recognized mud guard, starting to pick parts ...");
                        hal.pick("0");
                    }
                    //Wait for it to finish
                    partPickReceiver.expectMsgClass(Duration.ofSeconds(12000), PartsPickedNotification.class);
                    //hal.pick("0");
                }
            }
        };
    }

    @Test
    @Tag("UnitTest")
    public void testMonitoringReceivesEventOpcUa() {
        new TestKit(system) {
            {
                assertDoesNotThrow(() -> {
                    NodeId nodeId = NodeId.parse("ns=2;s=Monitoring/NotifyPartsPicked");
                    FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect("opc.tcp://127.0.0.1");
                    String response = client.callStringMethodBlocking(nodeId, new Variant("Cookie"), new Variant(DateTime.now().toString()), new Variant(String.valueOf(3)));
                    assertEquals("Ok", response);
                });
            }
        };
    }

    @Test
    @Tag("UnitTest")
    public void testOrderProcessForwarding() {
        new TestKit(system) {
            {
                monitorEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                File resourcesDirectory = new File("src/test/resources/process/xmiOrder20190419092949.xmi");
                ExtendedRegisterProcessRequest processRequest = loadExampleProcessFromFile(resourcesDirectory.getAbsolutePath(), getRef());

                monitoringActor.tell(processRequest, getRef());
                RegisterProcessRequest forwardedRequest = expectMsgClass(RegisterProcessRequest.class);
                assertEquals(processRequest.getRootOrderId(), forwardedRequest.getRootOrderId());
            }
        };
    }

    private ExtendedRegisterProcessRequest loadExampleProcessFromFile(String path, ActorRef sender) {
        String senderId = "MockOrderActor";
        XmlRoot xmlRoot = loadXmi(path);
        return new ExtendedRegisterProcessRequest(senderId, xmlRoot, sender);
    }

    public XmlRoot loadXmi(String path) {
        FileDataSource src = new FileDataSource(path);
        addPackages(src);
        XmlRoot root = null;
        try {
            root = src.getShopfloorData().get(0);
            return root;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }

    public void addPackages(FileDataSource src) {
        src.registerPackage(ActorCoreModelPackage.eNS_URI, ActorCoreModelPackage.eINSTANCE);
        src.registerPackage(ActorprocessPackage.eNS_URI, ActorprocessPackage.eINSTANCE);
        src.registerPackage(PartCoreModelPackage.eNS_URI, PartCoreModelPackage.eINSTANCE);
        src.registerPackage(LinkedCoreModelActorToPartPackage.eNS_URI, LinkedCoreModelActorToPartPackage.eINSTANCE);
        src.registerPackage(PartprocessPackage.eNS_URI, PartprocessPackage.eINSTANCE);
        src.registerPackage(ExtensionsForAssemblylinePackage.eNS_URI, ExtensionsForAssemblylinePackage.eINSTANCE);
        src.registerPackage(InstanceExtensionModelPackage.eNS_URI, InstanceExtensionModelPackage.eINSTANCE);
        src.registerPackage(ExtensionsCoreModelPackage.eNS_URI, ExtensionsCoreModelPackage.eINSTANCE);
        src.registerPackage(VariabilityExtensionModelPackage.eNS_URI, VariabilityExtensionModelPackage.eINSTANCE);
        src.registerPackage(PriorityExtensionModelPackage.eNS_URI, PriorityExtensionModelPackage.eINSTANCE);
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
