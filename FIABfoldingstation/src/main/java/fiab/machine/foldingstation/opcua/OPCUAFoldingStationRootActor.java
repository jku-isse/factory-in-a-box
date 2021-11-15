package fiab.machine.foldingstation.opcua;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.folding.FoldingMessageTypes;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.fu.HandshakeFU;
import fiab.handshake.fu.client.ClientSideHandshakeFU;
import fiab.handshake.fu.client.WiringUtils;
import fiab.handshake.fu.server.ServerSideHandshakeFU;
import fiab.machine.foldingstation.IntraMachineEventBus;
import fiab.machine.foldingstation.VirtualFoldingMachineActor;
import fiab.machine.foldingstation.events.MachineCapabilityUpdateEvent;
import fiab.machine.foldingstation.opcua.methods.FoldRequest;
import fiab.machine.foldingstation.opcua.methods.Reset;
import fiab.machine.foldingstation.opcua.methods.SetCapability;
import fiab.machine.foldingstation.opcua.methods.Stop;
import fiab.opcua.server.NonEncryptionBaseOpcUaServer;
import fiab.opcua.server.OPCUABase;
import fiab.opcua.server.PublicNonEncryptionBaseOpcUaServer;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.HashMap;
import java.util.Optional;

public class OPCUAFoldingStationRootActor extends AbstractActor {
    private String machineName = "FoldingStation";
    static final String NAMESPACE_URI = "urn:factory-in-a-box";
    private UaVariableNode status = null;
    private UaVariableNode capability = null;
    private ActorRef foldingCoordinator;
    private int portOffset;
    private HandshakeFU clientSideHandshakeOutput;


    static public Props props(String machineName, int portOffset) {
        return Props.create(OPCUAFoldingStationRootActor.class, () -> new OPCUAFoldingStationRootActor(machineName, portOffset));
    }

    public OPCUAFoldingStationRootActor(String machineName, int portOffset) {
        try {
            this.machineName = machineName;
            this.portOffset = portOffset;
            init();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void init() throws Exception {
        PublicNonEncryptionBaseOpcUaServer server1 = new PublicNonEncryptionBaseOpcUaServer(portOffset, machineName);

        OPCUABase opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
        UaFolderNode root = opcuaBase.prepareRootNode();
        UaFolderNode ttNode = opcuaBase.generateFolder(root, machineName, "Folding_FU");
        String fuPrefix = machineName + "/" + "Folding_FU";

        IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
        intraEventBus.subscribe(getSelf(), new fiab.machine.foldingstation.SubscriptionClassifier("Folding Module", "*"));
        foldingCoordinator = context().actorOf(VirtualFoldingMachineActor.propsForLateHandshakeBinding(intraEventBus), machineName);
        foldingCoordinator.tell(FoldingMessageTypes.SubscribeState, getSelf());

        HandshakeFU serverSideHandshake = new ServerSideHandshakeFU(opcuaBase, ttNode, fuPrefix, foldingCoordinator, getContext(), "DefaultServerSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, true);
        this.clientSideHandshakeOutput = new ClientSideHandshakeFU(opcuaBase, ttNode, fuPrefix, foldingCoordinator, getContext(), "DefaultClientSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_REQUIRED, false);
        setupFoldingCapabilities(opcuaBase, ttNode, fuPrefix);
        setupOPCUANodeSet(opcuaBase, ttNode, fuPrefix, foldingCoordinator);

        loadWiringFromFile();

        Thread s1 = new Thread(opcuaBase);
        s1.start();
    }

    private void loadWiringFromFile() {
        Optional<HashMap<String, WiringInfo>> optInfo = WiringUtils.loadWiringInfoFromFileSystem(machineName);
        if(clientSideHandshakeOutput != null && optInfo.isPresent()){
            try {
                clientSideHandshakeOutput.provideWiringInfo(optInfo.get().values().stream().findFirst().get());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .match(MachineCapabilityUpdateEvent.class, req -> {
                    setFoldCapability(req.getValue().toString());
                })
                .match(MachineStatusUpdateEvent.class, req -> {
                    setStatusValue(req.getStatus().toString());
                }).match(LocalEndpointStatus.LocalServerEndpointStatus.class, les -> {//ignore
                })
                .build();
    }

    private void setupOPCUANodeSet(OPCUABase opcuaBase, UaFolderNode ttNode, String path, ActorRef foldingActor) {
        //TODO rename ttNode
        UaMethodNode n1 = opcuaBase.createPartialMethodNode(path, FoldingMessageTypes.Reset.toString(), "Requests reset");
        opcuaBase.addMethodNode(ttNode, n1, new Reset(n1, foldingActor));
        UaMethodNode n2 = opcuaBase.createPartialMethodNode(path, FoldingMessageTypes.Stop.toString(), "Requests stop");
        opcuaBase.addMethodNode(ttNode, n2, new Stop(n2, foldingActor));
        UaMethodNode n3 = opcuaBase.createPartialMethodNode(path, FoldingMessageTypes.Fold.toString(), "Requests folding");
        opcuaBase.addMethodNode(ttNode, n3, new FoldRequest(n3, foldingActor));
        UaMethodNode n4 = opcuaBase.createPartialMethodNode(path, FoldingMessageTypes.SetCapability.toString(), "Sets Folding Capability");
        opcuaBase.addMethodNode(ttNode, n4, new SetCapability(n2, foldingActor));
        status = opcuaBase.generateStringVariableNode(ttNode, path, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, BasicMachineStates.UNKNOWN);
    }

    private void setupFoldingCapabilities(OPCUABase opcuaBase, UaFolderNode ttNode, String path) {
        // add capabilities
        UaFolderNode capabilitiesFolder = opcuaBase.generateFolder(ttNode, path, new String(OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
        path = path + "/" + OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
        UaFolderNode capability1 = opcuaBase.generateFolder(capabilitiesFolder, path,
                "CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);
        capability = opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
                WellknownFoldingCapability.generateFoldingCapabilityURI());
        opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                "DefaultFoldingCapabilityInstance");
        opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);
    }

    private void setStatusValue(String newStatus) {
        if (status != null) {
            status.setValue(new DataValue(new Variant(newStatus)));
        }
    }

    private void setFoldCapability(String newCapability) {
        if (capability != null) {
            capability.setValue(new DataValue(new Variant(newCapability)));
        }
    }

}
