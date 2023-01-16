package fiab.mes.assembly.display;

import ExtensionsForAssemblyline.AssemblyHumanStep;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.assembly.display.msg.ShowNextStepRequest;
import fiab.mes.assembly.order.message.ExtendedRegisterProcessRequest;
import fiab.mes.eventbus.DisplayEventBusWrapper;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;

import static fiab.mes.assembly.utils.AssemblyTestUtils.loadExampleProcessFromFile;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("IntegrationTest")
public class TestDisplayCurrentStep {

    private static final String displayEndpoint = "opc.tcp://127.0.0.1:4840";
    private ActorSystem system;
    private ActorRef displayActor;
    private ActorRef displayEventBus;

    @BeforeEach
    public void setup(){
        try {
            system = ActorSystem.create("DisplayTestSystem");
            displayEventBus = system.actorOf(DisplayEventBusWrapper.props(), DisplayEventBusWrapper.WRAPPER_ACTOR_LOOKUP_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void teardown(){
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testSystemDisplaysValue(){
        new TestKit(system){
            {
                assertDoesNotThrow(() -> {
                    FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect(displayEndpoint);
                    displayActor = system.actorOf(DisplayActor.props(client, "LocalTestDisplay"), "DisplayTestActor");

                    File resourcesDirectory = new File("src/test/resources/process/demoProc.xmi");
                    ExtendedRegisterProcessRequest processRequest = loadExampleProcessFromFile(resourcesDirectory.getAbsolutePath(), getRef());
                    AssemblyHumanStep step = (AssemblyHumanStep) processRequest.getXmlRoot().getProcesses().get(0).getSteps().get(0);
                    displayEventBus.tell(new ShowNextStepRequest(step, getRef()), getRef());
                    expectNoMessage(Duration.ofSeconds(5));
                });
            }
        };
    }
}
