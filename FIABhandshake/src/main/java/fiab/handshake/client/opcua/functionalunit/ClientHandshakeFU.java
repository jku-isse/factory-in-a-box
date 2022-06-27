package fiab.handshake.client.opcua.functionalunit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.client.ClientSideHandshakeActor;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.messages.WiringUpdateNotification;
import fiab.handshake.client.opcua.client.OpcUaServerHandshakeProxy;
import fiab.handshake.client.opcua.methods.*;
import fiab.handshake.client.statemachine.ClientSideHandshakeTriggers;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class ClientHandshakeFU extends ClientSideHandshakeActor {

    public static Props propsForStandaloneFU(OPCUABase base, UaFolderNode root, String handshakeId) {
        return props(base, root, handshakeId, new FUConnector(), new IntraMachineEventBus());
    }

    public static Props props(OPCUABase base, UaFolderNode root, String handshakeId,
                              FUConnector requestBus, IntraMachineEventBus intraMachineEventBus) {

        return Props.create(ClientHandshakeFU.class, () -> new ClientHandshakeFU(base, root, handshakeId,
                requestBus, intraMachineEventBus));
    }

    public static Props props(OPCUABase base, UaFolderNode root, String handshakeId,
                              FUConnector requestBus, IntraMachineEventBus intraMachineEventBus,
                              FUConnector serverRequestBus,
                              ServerResponseConnector serverResponseBus,
                              ServerNotificationConnector serverNotificationBus) {

        return Props.create(ClientHandshakeFU.class, () -> new ClientHandshakeFU(base, root, handshakeId,
                requestBus, intraMachineEventBus, serverRequestBus, serverResponseBus, serverNotificationBus));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected OPCUABase base;
    protected UaFolderNode rootNode;
    protected String fuPrefix;
    private ActorRef clientWrapper;

    private final String capabilityInstanceId;
    private final FUConnector serverRequestBus;
    private final ServerResponseConnector serverResponseBus;
    private final ServerNotificationConnector serverNotificationBus;

    protected UaVariableNode status = null;
    protected UaWiringInfoNodes wiringNodes;

    public ClientHandshakeFU(OPCUABase base, UaFolderNode root, String handshakeId,
                             FUConnector requestBus, IntraMachineEventBus intraMachineEventBus) {
        this(base, root, handshakeId, requestBus, intraMachineEventBus,
                new FUConnector(), new ServerResponseConnector(), new ServerNotificationConnector());
    }

    public ClientHandshakeFU(OPCUABase base, UaFolderNode root, String handshakeId,
                             FUConnector requestBus, IntraMachineEventBus intraMachineEventBus,
                             FUConnector serverRequestBus, ServerResponseConnector serverResponseBus,
                             ServerNotificationConnector serverNotificationBus) {
        super(requestBus, intraMachineEventBus, serverRequestBus, serverResponseBus, serverNotificationBus, false);
        this.base = base;
        this.rootNode = root;
        this.capabilityInstanceId = handshakeId;
        this.fuPrefix = createBasePathForFU(rootNode);
        this.serverRequestBus = serverRequestBus;
        this.serverResponseBus = serverResponseBus;
        this.serverNotificationBus = serverNotificationBus;
        createRemoteServerProxy();
        setupOpcUaNodeSet();
    }

    @Override
    protected void setWiringInfo(WiringInfo info) {
        super.setWiringInfo(info);
        uaUpdateWiringInfo(info);
    }

    @Override
    public void doResetting() {
        super.doResetting();
        if (this.wiringInfo != null) {
            this.clientWrapper.tell(new WiringRequest(componentId, wiringInfo), self());
        } else {
            log.warning("No WiringInfo set. Please set WiringInfo before resetting");
            this.stateMachine.fire(ClientSideHandshakeTriggers.STOP);
        }
    }

    private String createBasePathForFU(UaFolderNode rootNode) {
        return rootNode.getNodeId().getIdentifier() +
                "/" + HandshakeCapability.CLIENT_CAPABILITY_ID + "_" + capabilityInstanceId;
    }

    private void setupOpcUaNodeSet() {
        UaFolderNode handshakeFuNode = base.generateFolder(rootNode,
                HandshakeCapability.CLIENT_CAPABILITY_ID + "_" + capabilityInstanceId);

        status = base.generateStringVariableNode(handshakeFuNode,
                OPCUABasicMachineBrowsenames.STATE_VAR_NAME, ClientSideStates.STOPPED);

        addClientHandshakeOpcUaMethods(handshakeFuNode);
        addCapabilities(handshakeFuNode);
        addWiringInfo(handshakeFuNode);
    }

    private void addClientHandshakeOpcUaMethods(UaFolderNode clientFuNode) {
        UaMethodNode resetNode = base.createPartialMethodNode(clientFuNode,
                HandshakeCapability.OPCUA_INTERNAL_CLIENTSIDE_RESET_REQUEST, "Requests Reset");
        base.addMethodNode(clientFuNode, resetNode, new UaResetClientHandshake(resetNode, self()));
        UaMethodNode stopNode = base.createPartialMethodNode(clientFuNode,
                HandshakeCapability.OPCUA_INTERNAL_CLIENTSIDE_STOP_REQUEST, "Requests Stop");
        base.addMethodNode(clientFuNode, stopNode, new UaStopClientHandshake(stopNode, self()));
        UaMethodNode startNode = base.createPartialMethodNode(clientFuNode,
                HandshakeCapability.OPCUA_INTERNAL_CLIENTSIDE_START_REQUEST, "Begins Handshake Protocol");
        base.addMethodNode(clientFuNode, startNode, new UaStartClientHandshake(startNode, self()));
        UaMethodNode completeNode = base.createPartialMethodNode(clientFuNode,
                HandshakeCapability.OPCUA_INTERNAL_CLIENTSIDE_COMPLETE_REQUEST, "Completes Handshake Protocol");
        base.addMethodNode(clientFuNode, completeNode, new UaCompleteClientHandshake(completeNode, self()));
        UaMethodNode wiringNode = base.createPartialMethodNode(clientFuNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.OPCUA_WIRING_REQUEST, "Sets handshake wiring");
        base.addMethodNode(clientFuNode, wiringNode, new UaSetWiring(wiringNode, self()));
    }

    private void addCapabilities(UaFolderNode clientFuNode) {
        UaFolderNode capabilitiesFolder = base.generateFolder(clientFuNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES);

        addClientSideHandshakeCapability(capabilitiesFolder);
    }

    private void addWiringInfo(UaFolderNode clientFuNode) {
        UaFolderNode wiringFolder = base.generateFolder(clientFuNode, "WIRING_INFO");
        wiringNodes = new UaWiringInfoNodes();
        UaVariableNode localCapNode = base.generateStringVariableNode(wiringFolder, "LOCAL_CAP_ID", "");
        UaVariableNode remoteCapNode = base.generateStringVariableNode(wiringFolder, "REMOTE_CAP_ID", "");
        UaVariableNode remoteEndpointNode = base.generateStringVariableNode(wiringFolder, "REMOTE_ENDPOINT_URL", "");
        UaVariableNode remoteNodeIdNode = base.generateStringVariableNode(wiringFolder, "REMOTE_NODE_ID", "");
        UaVariableNode remoteRoleNode = base.generateStringVariableNode(wiringFolder, "REMOTE_ROLE", "");

        wiringNodes.setLocalCapabilityIdNode(localCapNode);
        wiringNodes.setRemoteCapabilityIdNode(remoteCapNode);
        wiringNodes.setRemoteCapabilityURLNode(remoteEndpointNode);
        wiringNodes.setRemoteNodeIdNode(remoteNodeIdNode);
        wiringNodes.setRemoteRoleNode(remoteRoleNode);
    }

    private void uaUpdateWiringInfo(WiringInfo wiringInfo) {
        wiringNodes.getLocalCapabilityIdNode().setValue(new DataValue(new Variant(wiringInfo.getLocalCapabilityId())));
        wiringNodes.getRemoteCapabilityIdNode().setValue(new DataValue(new Variant(wiringInfo.getRemoteCapabilityId())));
        wiringNodes.getRemoteCapabilityURLNode().setValue(new DataValue(new Variant(wiringInfo.getRemoteEndpointURL())));
        wiringNodes.getRemoteNodeIdNode().setValue(new DataValue(new Variant(wiringInfo.getRemoteNodeId())));
        wiringNodes.getRemoteRoleNode().setValue(new DataValue(new Variant(wiringInfo.getRemoteRole())));
        intraMachineEventBus.publish(new WiringUpdateNotification(componentId, wiringInfo));
    }

    private void addClientSideHandshakeCapability(UaFolderNode capabilitiesFolder) {
        UaFolderNode clientHandshakeCapNode = base.generateFolder(capabilitiesFolder,
                OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);

        base.generateStringVariableNode(clientHandshakeCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
                HandshakeCapability.HANDSHAKE_CAPABILITY_URI);
        base.generateStringVariableNode(clientHandshakeCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                capabilityInstanceId);
        base.generateStringVariableNode(clientHandshakeCapNode,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_REQUIRED);
    }

    private void createRemoteServerProxy() {
        this.clientWrapper = context()
                .actorOf(OpcUaServerHandshakeProxy.props(self(),
                                serverRequestBus, serverResponseBus, serverNotificationBus),
                        capabilityInstanceId + "ServerProxy");
    }

    @Override
    public void setStatusValue(String newStatus) {
        super.setStatusValue(newStatus);
        if (status != null) {
            status.setValue(new DataValue(new Variant(newStatus)));
        }
    }

    //TODO move to own java file
    static class UaWiringInfoNodes {
        private UaVariableNode localCapabilityIdNode;
        private UaVariableNode remoteCapabilityIdNode;
        private UaVariableNode remoteCapabilityURLNode;
        private UaVariableNode remoteNodeIdNode;
        private UaVariableNode remoteRoleNode;

        public UaVariableNode getLocalCapabilityIdNode() {
            return localCapabilityIdNode;
        }

        public void setLocalCapabilityIdNode(UaVariableNode localCapabilityIdNode) {
            this.localCapabilityIdNode = localCapabilityIdNode;
        }

        public UaVariableNode getRemoteCapabilityIdNode() {
            return remoteCapabilityIdNode;
        }

        public void setRemoteCapabilityIdNode(UaVariableNode remoteCapabilityIdNode) {
            this.remoteCapabilityIdNode = remoteCapabilityIdNode;
        }

        public UaVariableNode getRemoteCapabilityURLNode() {
            return remoteCapabilityURLNode;
        }

        public void setRemoteCapabilityURLNode(UaVariableNode remoteCapabilityURLNode) {
            this.remoteCapabilityURLNode = remoteCapabilityURLNode;
        }

        public UaVariableNode getRemoteNodeIdNode() {
            return remoteNodeIdNode;
        }

        public void setRemoteNodeIdNode(UaVariableNode remoteNodeIdNode) {
            this.remoteNodeIdNode = remoteNodeIdNode;
        }

        public UaVariableNode getRemoteRoleNode() {
            return remoteRoleNode;
        }

        public void setRemoteRoleNode(UaVariableNode remoteRoleNode) {
            this.remoteRoleNode = remoteRoleNode;
        }
    }

    //private WiringInfo createServerHandshakeWiringInfo(){
    //        return new WiringInfoBuilder()
    //                .setLocalCapabilityId("NORTH_CLIENT")
    //                .setRemoteCapabilityId("DefaultHandshakeServerSide")
    //                .setRemoteEndpointURL("opc.tcp://127.0.0.1:4840/milo")
    //                .setRemoteNodeId("ns=2;s=Handshake/ServerHandshake/HANDSHAKE_FU_DefaultServerSideHandshake/CAPABILITIES/CAPABILITY")
    //                .setRemoteRole("RemoteRole1")
    //                .build();
    //    }
}
