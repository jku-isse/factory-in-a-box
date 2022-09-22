package fiab.mes.assembly.monitoring;

import ActorCoreModel.ActorCoreModelPackage;
import ExtensionsCoreModel.ExtensionsCoreModelPackage;
import ExtensionsForAssemblyline.ExtensionsForAssemblylinePackage;
import InstanceExtensionModel.InstanceExtensionModelPackage;
import LinkedCoreModelActorToPart.LinkedCoreModelActorToPartPackage;
import PartCoreModel.PartCoreModelPackage;
import PriorityExtensionModel.PriorityExtensionModelPackage;
import ProcessCore.Process;
import ProcessCore.ProcessStep;
import ProcessCore.XmlRoot;
import VariabilityExtensionModel.VariabilityExtensionModelPackage;
import actorprocess.ActorprocessPackage;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import at.pro2future.shopfloors.interfaces.impl.FileDataSource;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.assembly.monitoring.actor.AssemblyMonitoringActor;
import fiab.mes.assembly.monitoring.message.PartsPickedNotification;
import fiab.mes.assembly.order.message.ExtendedRegisterProcessRequest;
import fiab.mes.eventbus.*;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMonitoringActor {

    private ActorSystem system;
    private ActorRef monitoringActor;
    private ActorRef monitorEventBus;

    public static void main(String[] args) {

    }

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
}
