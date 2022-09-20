package fiab.mes.assembly.monitoring;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.assembly.monitoring.actor.AssemblyMonitoringActor;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMonitoringActor {

    private ActorSystem system;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create("MonitoringUnitTest");
        system.actorOf(AssemblyMonitoringActor.props(OPCUABase.createAndStartLocalServer(4840, "Monitoring")));
    }

    @AfterEach
    public void teardown(){
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testMonitoringReceivesEvent() {
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
}
