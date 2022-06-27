package fiab.turntable.opcua;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.fu.HandshakeFU;
import fiab.handshake.fu.client.ClientSideHandshakeFU;
import fiab.handshake.fu.client.WiringUtils;
import fiab.handshake.fu.server.ServerSideHandshakeFU;
import fiab.opcua.server.FastPublicNonEncryptionBaseOpcUaServer;
import fiab.opcua.server.OPCUABase;
//import fiab.turntable.messages.TransportModuleRequest;
import fiab.turntable.actor.NoOpTransportModuleCoordinator;
import fiab.turntable.actor.TransportModuleCoordinatorActor;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.turntable.opcua.methods.UaResetTurntable;
import fiab.turntable.opcua.methods.UaStopTurntable;
import fiab.turntable.opcua.methods.UaTransportRequest;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.HashMap;
import java.util.Optional;

public class OPCUATurntableRootActor extends AbstractActor {

    private String machineName = "Turntable";
    static final String NAMESPACE_URI = "urn:factory-in-a-box";
    private HashMap<String, HandshakeFU> handshakeFUs = new HashMap<>();
    private UaVariableNode status = null;
    private ActorRef ttWrapper;
    private int portOffset;
    private boolean exposeInternalControl = false;

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static public Props props(String machineName, int portOffset, boolean exposeInternalControl) {
        return Props.create(OPCUATurntableRootActor.class, () -> new OPCUATurntableRootActor(machineName, portOffset, exposeInternalControl));
    }

    public OPCUATurntableRootActor(String machineName, int portOffset, boolean exposeInternalControl) {
        try {
            this.machineName = machineName;
            this.portOffset = portOffset;
            this.exposeInternalControl = exposeInternalControl;
            init();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .match(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.class, req -> {
                    if (ttWrapper != null) ttWrapper.tell(req, getSelf());
                })
                .match(MachineStatusUpdateEvent.class, req -> {
                    setStatusValue(req.getStatus().toString());
                })
                /*.match(TransportModuleRequest.class, req -> {
                    // forward to return response directly into method call back
                    if (ttWrapper != null) ttWrapper.forward(req, getContext());
                })*/
                //.matchAny(msg -> log.info("Received unknown message " + msg + " from " + sender()))
                .build();
    }


    private void init() throws Exception {
        OPCUABase opcuaBase;
        /*if (System.getProperty("os.name").contains("win")) {
            NonEncryptionBaseOpcUaServer server1 = new NonEncryptionBaseOpcUaServer(portOffset, machineName);
            opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
        } else {*/
            //PublicNonEncryptionBaseOpcUaServer server1 = new PublicNonEncryptionBaseOpcUaServer(portOffset, machineName);
        FastPublicNonEncryptionBaseOpcUaServer server1 = new FastPublicNonEncryptionBaseOpcUaServer(portOffset, machineName);
            opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
        //}
        //OPCUABase opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
        UaFolderNode root = opcuaBase.prepareRootNode();
        UaFolderNode ttNode = opcuaBase.generateFolder(root, machineName, "Turntable_FU");
        String fuPrefix = machineName + "/" + "Turntable_FU";

        IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
        intraEventBus.subscribe(getSelf(), new FUSubscriptionClassifier("TurntableRoot", "*"));
//       ttWrapper = context().actorOf(TransportModuleCoordinatorActor.props(intraEventBus,
//                context().actorOf(TurntableActor.props(intraEventBus, null)),
//                context().actorOf(ConveyorActor.props(intraEventBus, null))), "TurntableCoordinator");
//        ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, getSelf());
        //ttWrapper.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, getSelf());


		if (!exposeInternalControl) {
			//TurningFU turningFU = new TurningFU(opcuaBase, ttNode, fuPrefix, getContext(), exposeInternalControl, intraEventBus);
			//ConveyorFU conveyorFU = new ConveyorFU(opcuaBase, ttNode, fuPrefix, getContext(), exposeInternalControl, intraEventBus);
			ttWrapper = context().actorOf(TransportModuleCoordinatorActor.props(intraEventBus,
		                //context().actorOf(TurntableActor.props(intraEventBus, null), "TurntableFU"),
		                //context().actorOf(ConveyorActor.props(intraEventBus, null), "ConveyingFU")), 
						null, //turningFU.getActor(),
						null), "TTCoord");//conveyorFU.getActor()),	"TurntableCoordinator");
            ttWrapper.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, getSelf());
			//ttWrapper.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, getSelf());
		} else {
			ttWrapper = context().actorOf(NoOpTransportModuleCoordinator.props(), "NoOpTT");
			//TurningFU turningFU = new TurningFU(opcuaBase, ttNode, fuPrefix, getContext(), exposeInternalControl, null);
			//ConveyorFU conveyorFU = new ConveyorFU(opcuaBase, ttNode, fuPrefix, getContext(), exposeInternalControl, null);
		}        

        setupTurntableCapabilities(opcuaBase, ttNode, fuPrefix);
        setupOPCUANodeSet(opcuaBase, ttNode, fuPrefix, ttWrapper);

        // there is always a west, south, north, client
        HandshakeFU westFU = new ClientSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, false, exposeInternalControl);
        handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT,
                westFU);
        HandshakeFU southFU = new ClientSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT, false, exposeInternalControl);
        handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT,
                southFU);
        HandshakeFU northFU = new ClientSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, false, exposeInternalControl);
        handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT,
                northFU);

        // we can have server and client set up regardless of shopfloor location, TODO: setup client and server for N, S, E, W 
        //if (machineName.equalsIgnoreCase("Turntable1")) { // we have a server here
        HandshakeFU eastServerFU = new ServerSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER, true, exposeInternalControl);
        handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER,
                eastServerFU);
        //} else { // we have a client here
        HandshakeFU eastClientFU = new ClientSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, false, exposeInternalControl);
        handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT,
                eastClientFU);
        //}


        loadWiringFromFile();

//		OPCUAInputStationMock ism1 = new OPCUAInputStationMock(server1.getServer(), NAMESPACE_URI, "InputStation", actor1, HandshakeProtocol.INPUTSTATION_CAPABILITY_URI);
//		// differentiate in/out
        Thread s1 = new Thread(opcuaBase);
        s1.start();
    }

    private void loadWiringFromFile() {
        Optional<HashMap<String, WiringInfo>> optInfo = WiringUtils.loadWiringInfoFromFileSystem(machineName);
        optInfo.ifPresent(info -> {
            info.values().stream()
                    .filter(wi -> handshakeFUs.containsKey(wi.getLocalCapabilityId()))
                    .forEach(wi -> {
                        try {
                            handshakeFUs.get(wi.getLocalCapabilityId()).provideWiringInfo(wi);
                        } catch (Exception e) {
                            log.warning("Error applying wiring info " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
        });
    }

    private void setupOPCUANodeSet(OPCUABase opcuaBase, UaFolderNode ttNode, String path, ActorRef ttActor) {

        UaMethodNode n1 = opcuaBase.createPartialMethodNode(path, TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset.toString(), "Requests reset");
        opcuaBase.addMethodNode(ttNode, n1, new UaResetTurntable(n1, ttActor));
        UaMethodNode n2 = opcuaBase.createPartialMethodNode(path, TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Stop.toString(), "Requests stop");
        opcuaBase.addMethodNode(ttNode, n2, new UaStopTurntable(n2, ttActor));
        UaMethodNode n3 = opcuaBase.createPartialMethodNode(path, TransportModuleCapability.OPCUA_TRANSPORT_REQUEST, "Requests transport");
        opcuaBase.addMethodNode(ttNode, n3, new UaTransportRequest(n3, ttActor));
        status = opcuaBase.generateStringVariableNode(ttNode, path, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, BasicMachineStates.UNKNOWN);
    }

    private void setupTurntableCapabilities(OPCUABase opcuaBase, UaFolderNode ttNode, String path) {
        // add capabilities
        UaFolderNode capabilitiesFolder = opcuaBase.generateFolder(ttNode, path, new String(OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
        path = path + "/" + OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
        UaFolderNode capability1 = opcuaBase.generateFolder(capabilitiesFolder, path,
                "CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);
        opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
                new String(TransportModuleCapability.TRANSPORT_CAPABILITY_URI));
        opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
                new String("DefaultTurntableCapabilityInstance"));
        opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
                new String(OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED));
    }

    private void setStatusValue(String newStatus) {
        if (status != null) {
            status.setValue(new DataValue(new Variant(newStatus)));
        }
    }
}
