package fiab.handshake.server.opcua.functionalunit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.ClientProxyActor;
import fiab.handshake.server.ServerSideHandshakeActor;
import fiab.handshake.server.opcua.methods.*;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class ServerHandshakeFU extends ServerSideHandshakeActor {

    public static Props propsForStandaloneFU(OPCUABase base, UaFolderNode root) {
        return props(base, root, "", new FUConnector(), new IntraMachineEventBus());
    }

    public static Props propsForStandaloneFU(OPCUABase base, UaFolderNode root, String handshakeId) {
        return props(base, root, handshakeId, new FUConnector(), new IntraMachineEventBus());
    }

    public static Props props(OPCUABase base, UaFolderNode root, String handshakeId,
                              FUConnector requestBus, IntraMachineEventBus intraMachineEventBus) {
        return Props.create(ServerHandshakeFU.class,
                () -> new ServerHandshakeFU(base, root, handshakeId, requestBus, intraMachineEventBus));
    }

    protected final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    protected OPCUABase base;
    protected UaFolderNode rootNode;
    protected UaFolderNode capabilitiesFolder;
    protected String fuPrefix;

    private final String capabilityInstanceId;
    private final IntraMachineEventBus intraMachineEventBus;
    private final FUConnector serverRequestBus;
    private ActorRef clientProxy;

    protected UaVariableNode status = null;

    public ServerHandshakeFU(OPCUABase base, UaFolderNode root, String handshakeId,
                             FUConnector requestBus, IntraMachineEventBus intraMachineEventBus) {
        super(requestBus, intraMachineEventBus, new ServerResponseConnector(), new ServerNotificationConnector());
        this.base = base;
        this.capabilityInstanceId = handshakeId;
        this.fuPrefix = createBasePathForFU(root);
        this.intraMachineEventBus = intraMachineEventBus;
        this.serverRequestBus = requestBus;
        createClientProxy();
        setupOpcUaNodeSet(root);
    }

    private void createClientProxy() {
        Props proxyProps = ClientProxyActor.props(serverRequestBus, responseConnector);
        this.clientProxy = context().actorOf(proxyProps, HandshakeCapability.SERVER_CAPABILITY_ID + capabilityInstanceId + "ClientProxy");
    }

    private String createBasePathForFU(UaFolderNode serverRootNode) {
        return serverRootNode.getNodeId().getIdentifier() +
                "/" + HandshakeCapability.SERVER_CAPABILITY_ID + "_" + capabilityInstanceId;
    }

    protected void setupOpcUaNodeSet(UaFolderNode serverRootNode) {
        String folderName = HandshakeCapability.SERVER_CAPABILITY_ID;
        if (!capabilityInstanceId.isBlank()) folderName = folderName + "_" + capabilityInstanceId;
        this.rootNode = base.generateFolder(serverRootNode, folderName);

        status = base.generateStringVariableNode(rootNode,
                OPCUABasicMachineBrowsenames.STATE_VAR_NAME, ClientSideStates.STOPPED);

        addServerHandshakeOpcUaMethods(rootNode);
        addCapabilities(rootNode);
    }

    protected void addServerHandshakeOpcUaMethods(UaFolderNode serverFuNode) {
        UaMethodNode resetNode = base.createPartialMethodNode(serverFuNode,
                HandshakeCapability.OPCUA_INTERNAL_SERVERSIDE_RESET_REQUEST, "Requests Reset");
        base.addMethodNode(serverFuNode, resetNode, new UaResetServerHandshake(resetNode, self()));
        UaMethodNode stopNode = base.createPartialMethodNode(serverFuNode,
                HandshakeCapability.OPCUA_INTERNAL_SERVERSIDE_STOP_REQUEST, "Requests Stop");
        base.addMethodNode(serverFuNode, stopNode, new UaStopServerHandshake(stopNode, self()));
        UaMethodNode initNode = base.createPartialMethodNode(serverFuNode,
                HandshakeCapability.OPCUA_EXTERNAL_SERVERSIDE_INIT_HANDOVER_REQUEST, "Initiates Handshake");
        base.addMethodNode(serverFuNode, initNode, new UaInitiateServerHandshake(initNode, this.clientProxy));
        UaMethodNode startNode = base.createPartialMethodNode(serverFuNode,
                HandshakeCapability.OPCUA_EXTERNAL_SERVERSIDE_START_HANDOVER_REQUEST, "Starts Handshake Protocol");
        base.addMethodNode(serverFuNode, startNode, new UaStartServerHandshake(startNode, this.clientProxy));
        UaMethodNode completeNode = base.createPartialMethodNode(serverFuNode,
                HandshakeCapability.OPCUA_INTERNAL_SERVERSIDE_COMPLETE_REQUEST, "Completes Handshake Protocol");
        base.addMethodNode(serverFuNode, completeNode, new UaCompleteServerHandshake(completeNode, self()));
        UaMethodNode setEmptyNode = base.createPartialMethodNode(serverFuNode,
                HandshakeCapability.StateOverrideRequests.SetEmpty.name(), "Overrides Transport Area Status to Empty");
        base.addMethodNode(serverFuNode, setEmptyNode, new UaSetEmpty(setEmptyNode, self()));
        UaMethodNode setLoadedNode = base.createPartialMethodNode(serverFuNode,
                HandshakeCapability.StateOverrideRequests.SetLoaded.name(), "Overrides Transport Area Status to Loaded");
        base.addMethodNode(serverFuNode, setLoadedNode, new UaSetLoaded(setEmptyNode, self()));
    }

    protected void addCapabilities(UaFolderNode serverFuNode) {
        capabilitiesFolder = base.generateFolder(serverFuNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES);

        addServerSideHandshakeCapability(capabilitiesFolder);
    }

    protected void addServerSideHandshakeCapability(UaFolderNode capabilitiesFolder) {
        String capabilityId = !capabilityInstanceId.isBlank() ? capabilityInstanceId : "DefaultHandshakeServerSide";
        UaFolderNode serverHandshakeCapNode = base.generateFolder(capabilitiesFolder,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);

        base.generateStringVariableNode(serverHandshakeCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
                HandshakeCapability.HANDSHAKE_CAPABILITY_URI);
        base.generateStringVariableNode(serverHandshakeCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                capabilityId);
        base.generateStringVariableNode(serverHandshakeCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);
    }

    @Override
    public void setStatusValue(String newStatus) {
        super.setStatusValue(newStatus);
        if (status != null) {
            status.setValue(new DataValue(new Variant(newStatus)));
        }
    }
}
