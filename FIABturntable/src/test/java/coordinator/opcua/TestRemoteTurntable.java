package coordinator.opcua;

import akka.actor.ActorSystem;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.turntable.TurntableFactory;
import org.junit.jupiter.api.*;

@Tag("SystemTest")
public class TestRemoteTurntable {  //TODO test remote machine

    private FiabOpcUaClient client;

    @BeforeEach
    public void setup(){
        try {
            client = OPCUAClientFactory.createFIABClientAndConnect("opc.tcp://127.0.0.1:4840");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void teardown(){
        client.disconnectClient();
    }

    @Test
    public void testRemoteReset(){

    }


}
