package coordinator.opcua;

import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import org.junit.jupiter.api.*;

@Tag("SystemTest")
public class TestRemoteTurntable {

    private FiabOpcUaClient client;

    @BeforeAll
    public static void init(){
        //TODO
    }

    @BeforeEach
    public void setup(){
        try {
            client = OPCUAClientFactory.createFIABClient("opc.tcp://127.0.0.1:4840");
            client.connectFIABClient().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void teardown(){

    }

    @AfterAll
    public static void cleanup(){

    }

    @Test
    public void testRemoteReset(){

    }
}
