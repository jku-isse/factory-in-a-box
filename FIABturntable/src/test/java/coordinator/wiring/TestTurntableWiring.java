package coordinator.wiring;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.messages.WiringUpdateNotification;
import fiab.handshake.fu.client.ServerHandshakeNodeIds;
import fiab.handshake.server.opcua.functionalunit.ServerHandshakeFU;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.message.*;
import fiab.turntable.opcua.OpcUaTurntableActor;
import fiab.turntable.wiring.WiringActor;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.Duration;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("UnitTest")
public class TestTurntableWiring {

    private ActorSystem system;

    private TestKit probe;
    private ActorRef wiringActor;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create();
        probe = new TestKit(system);
        wiringActor = system.actorOf(WiringActor.props(probe.getRef(), "VirtualTurntable"));
        probe.expectMsgClass(WiringRequest.class);  //Since we have a wiring, this message will be sent when actor starts
    }

    @AfterEach
    public void teardown() {
        wiringActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        system.terminate();
    }

    @Test
    public void testWiringFromFileSuccessful() {
        wiringActor.tell(new ApplyWiringFromFile("VirtualTurntable"), probe.getRef());

        WiringInfo wiringInfo = probe.expectMsgClass(Duration.ofSeconds(10), WiringRequest.class).getInfo();

        assertEquals("NORTH_CLIENT", wiringInfo.getLocalCapabilityId());
        assertEquals("NORTH_SERVER", wiringInfo.getRemoteCapabilityId());
        assertEquals("opc.tcp://127.0.0.1:4841", wiringInfo.getRemoteEndpointURL());
        assertEquals("ns=2;s=HandshakeDevice/HANDSHAKE_FU_NORTH_SERVER/CAPABILITIES/CAPABILITY", wiringInfo.getRemoteNodeId());
        assertEquals("RemoteRole1", wiringInfo.getRemoteRole());
    }

    @Test
    public void testWiringChangeIsPersistedInFile() {
        WiringInfo wiringInfo = new WiringInfoBuilder()
                .setLocalCapabilityId("SOUTH_CLIENT")
                .setRemoteCapabilityId("SOUTH_SERVER")
                .setRemoteEndpointURL("opc.tcp://127.0.0.1:4842")
                .setRemoteNodeId("ns=2;s=HandshakeDevice/HANDSHAKE_FU_NORTH_SERVER/CAPABILITIES/CAPABILITY")
                .setRemoteRole("RemoteRole1")
                .build();
        wiringActor.tell(new WiringUpdateNotification("SOUTH_CLIENT", wiringInfo), probe.getRef());
        wiringActor.tell(new SaveWiringToFile("TestVirtualTurntable"), probe.getRef());
        probe.expectMsgClass(WiringSavedNotification.class);
        File updatedWiringInfoFile = new File("TestVirtualTurntablewiringinfo.json");
        assertTrue(updatedWiringInfoFile.exists());

        wiringActor.tell(new ApplyWiringFromFile("TestVirtualTurntable"), probe.getRef());
        probe.expectMsgClass(WiringRequest.class);
        probe.expectMsgClass(WiringRequest.class);
        updatedWiringInfoFile.deleteOnExit();
    }

    @Test
    public void testSaveWiringWithoutChangeDoesNotAlterContents() {
        wiringActor.tell(new SaveWiringToFile("VirtualTurntable"), probe.getRef());
        probe.expectMsgClass(WiringSavedNotification.class);

        wiringActor.tell(new ApplyWiringFromFile("VirtualTurntable"), probe.getRef());

        WiringInfo wiringInfo = probe.expectMsgClass(WiringRequest.class).getInfo();

        assertEquals("NORTH_CLIENT", wiringInfo.getLocalCapabilityId());
        assertEquals("NORTH_SERVER", wiringInfo.getRemoteCapabilityId());
        assertEquals("opc.tcp://127.0.0.1:4841", wiringInfo.getRemoteEndpointURL());
        assertEquals("ns=2;s=HandshakeDevice/HANDSHAKE_FU_NORTH_SERVER/CAPABILITIES/CAPABILITY", wiringInfo.getRemoteNodeId());
        assertEquals("RemoteRole1", wiringInfo.getRemoteRole());
    }

    @Test
    public void testDeleteWiringInfo() {
        String wiringFileName = "TestVirtualTurntable";
        WiringInfo wiringInfo = new WiringInfoBuilder()
                .setLocalCapabilityId("SOUTH_CLIENT")
                .setRemoteCapabilityId("SOUTH_SERVER")
                .setRemoteEndpointURL("opc.tcp://127.0.0.1:4842")
                .setRemoteNodeId("ns=2;s=HandshakeDevice/HANDSHAKE_FU_NORTH_SERVER/CAPABILITIES/CAPABILITY")
                .setRemoteRole("RemoteRole1")
                .build();
        wiringActor.tell(new WiringUpdateNotification("SOUTH_CLIENT", wiringInfo), probe.getRef());
        wiringActor.tell(new SaveWiringToFile(wiringFileName), probe.getRef());
        probe.expectMsgClass(WiringSavedNotification.class);
        File updatedWiringInfoFile = new File(wiringFileName + "wiringinfo.json");
        assertTrue(updatedWiringInfoFile.exists());

        wiringActor.tell(new DeleteWiringInfoFile(wiringFileName), probe.getRef());
        probe.expectMsgClass(WiringDeletedNotification.class);
        assertFalse(updatedWiringInfoFile.exists());
        updatedWiringInfoFile.deleteOnExit();
    }
}
