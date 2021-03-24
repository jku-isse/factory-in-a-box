package fiab.turntable.opcua;

import java.util.HashMap;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import config.HardwareInfo;
import config.MachineType;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.fu.HandshakeFU;
import fiab.handshake.fu.client.ClientSideHandshakeFU;
import fiab.handshake.fu.server.ServerSideHandshakeFU;
import fiab.opcua.server.NonEncryptionBaseOpcUaServer;
import fiab.opcua.server.OPCUABase;
import fiab.opcua.server.PublicNonEncryptionBaseOpcUaServer;
import fiab.tracing.actor.AbstractTracingActor;
import fiab.turntable.actor.InternalTransportModuleRequest;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.actor.NoOpTransportModuleCoordinator;
import fiab.turntable.actor.SubscriptionClassifier;
import fiab.turntable.actor.TransportModuleCoordinatorActor;
import fiab.turntable.actor.WiringUpdateEvent;
import fiab.turntable.actor.messages.TTModuleWellknwonCapabilityIdentifierMessage;
import fiab.turntable.conveying.fu.opcua.ConveyingFU;
import fiab.turntable.opcua.methods.Reset;
import fiab.turntable.opcua.methods.Stop;
import fiab.turntable.opcua.methods.TransportRequest;
import fiab.turntable.turning.fu.opcua.TurningFU;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;

public class OPCUATurntableRootActor extends AbstractTracingActor {

	private String machineName = "Turntable";
	private String wiringFilePath = "";
	static final String NAMESPACE_URI = "urn:factory-in-a-box";
	private HashMap<String, HandshakeFU> handshakeFUs = new HashMap<>();
	private UaVariableNode status = null;
	private ActorRef ttWrapper;
	private int portOffset;
	private boolean exposeInternalControl = false;

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	static public Props props(String machineName, int portOffset, boolean exposeInternalControl,
			AsyncReporter<zipkin2.Span> reporter) {
		return Props.create(OPCUATurntableRootActor.class,
				() -> new OPCUATurntableRootActor(machineName, "", portOffset, exposeInternalControl, reporter));
	}

	static public Props props(String machineName, String wiringFilePath, int portOffset, boolean exposeInternalControl,
			AsyncReporter<zipkin2.Span> reporter) {
		return Props.create(OPCUATurntableRootActor.class, () -> new OPCUATurntableRootActor(machineName,
				wiringFilePath, portOffset, exposeInternalControl, reporter));
	}

	public OPCUATurntableRootActor(String machineName, String wiringFilePath, int portOffset,
			boolean exposeInternalControl, AsyncReporter<zipkin2.Span> reporter) {
		try {
			this.wiringFilePath = wiringFilePath;
			this.machineName = machineName;
			this.portOffset = portOffset;
			this.exposeInternalControl = exposeInternalControl;
			init(reporter);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(TTModuleWellknwonCapabilityIdentifierMessage.class, msg -> {
			receiveTTModuleWellknownCapabilityIdentifier(msg);
		}).match(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.class, req -> {
			receiveTTModuleWellknownCapabilityIdentifier(new TTModuleWellknwonCapabilityIdentifierMessage("", req));
		}).match(MachineStatusUpdateEvent.class, req -> {
			try {
				tracer.startConsumerSpan(req, "OPCUA Turntable Root Actor: Machine Status Update Event received");
				setStatusValue(req.getStatus().toString());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracer.finishCurrentSpan();
			}

		}).match(InternalTransportModuleRequest.class, req -> {
			// forward to return response directly into method call back
			try {
				tracer.startConsumerSpan(req, "OPCUA Turntable Root Actor: Internal Transport Module Request received");
				if (ttWrapper != null)
					ttWrapper.forward(req, getContext());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracer.finishCurrentSpan();
			}

		}).match(WiringUpdateEvent.class, req -> {
			try {
				tracer.startConsumerSpan(req, "OPCUA Turntable Root Actor: Wiring Update Event received");
				writeWiringToFile();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracer.finishCurrentSpan();
			}

		}).build();
	}

	private void receiveTTModuleWellknownCapabilityIdentifier(TTModuleWellknwonCapabilityIdentifierMessage msg) {

		try {
			tracer.startConsumerSpan(msg,
					"OPCUA Turntable Root Actor: TT Module Wellknow Capability Identifier received");
			if (ttWrapper != null) {
				TTModuleWellknwonCapabilityIdentifierMessage newMsg = new TTModuleWellknwonCapabilityIdentifierMessage(
						tracer.getCurrentHeader(), msg.getBody());
				tracer.injectMsg(newMsg);
				ttWrapper.tell(newMsg, getSelf());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			tracer.finishCurrentSpan();
		}

	}

	private void init(AsyncReporter<Span> reporter) throws Exception {
		OPCUABase opcuaBase;
		if (System.getProperty("os.name").contains("win")) {
			NonEncryptionBaseOpcUaServer server1 = new NonEncryptionBaseOpcUaServer(portOffset, machineName);
			opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
		} else {
			PublicNonEncryptionBaseOpcUaServer server1 = new PublicNonEncryptionBaseOpcUaServer(portOffset,
					machineName);
			opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
		}

		UaFolderNode root = opcuaBase.prepareRootNode();
		UaFolderNode ttNode = opcuaBase.generateFolder(root, machineName, "Turntable_FU");
		String fuPrefix = machineName + "/" + "Turntable_FU";

		IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
		intraEventBus.subscribe(getSelf(), new SubscriptionClassifier("TurntableRoot", "*"));
		HardwareInfo hardwareInfo = new HardwareInfo(MachineType.TURNTABLE);
		if (!exposeInternalControl) {
			TurningFU turningFU = new TurningFU(opcuaBase, ttNode, fuPrefix, getContext(), exposeInternalControl,
					intraEventBus, hardwareInfo);
			ConveyingFU conveyorFU = new ConveyingFU(opcuaBase, ttNode, fuPrefix, getContext(), exposeInternalControl,
					intraEventBus, hardwareInfo);
			ttWrapper = context().actorOf(
					TransportModuleCoordinatorActor.props(intraEventBus, turningFU.getActor(), conveyorFU.getActor()),
					"TurntableCoordinator");
			ttWrapper.tell(
					new TTModuleWellknwonCapabilityIdentifierMessage(tracer.getCurrentHeader(),
							TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState),
					getSelf());
		} else {
			ttWrapper = context().actorOf(NoOpTransportModuleCoordinator.props(), "NoOpTT");
			TurningFU turningFU = new TurningFU(opcuaBase, ttNode, fuPrefix, getContext(), exposeInternalControl, null,
					hardwareInfo);
			ConveyingFU conveyorFU = new ConveyingFU(opcuaBase, ttNode, fuPrefix, getContext(), exposeInternalControl,
					null, hardwareInfo);
		}

		setupTurntableCapabilities(opcuaBase, ttNode, fuPrefix);
		setupOPCUANodeSet(opcuaBase, ttNode, fuPrefix, ttWrapper);

		// Add hardware info
		context().actorOf(OPCUATurntableHardwareMonitor.props(opcuaBase, ttNode, fuPrefix, hardwareInfo));

		// there is always a west, south, north, client
		HandshakeFU westFU = new ClientSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(),
				TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, false,
				exposeInternalControl, reporter);
		handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, westFU);

		HandshakeFU southFU = new ClientSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(),
				TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT, false,
				exposeInternalControl, reporter);
		handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT, southFU);

		HandshakeFU northFU = new ClientSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(),
				TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, false,
				exposeInternalControl, reporter);
		handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, northFU);

		// we can have server and client set up regardless of shopfloor location, TODO:
		// setup client and server for N, S, E, W
		// if (machineName.equalsIgnoreCase("Turntable1")) { // we have a server here
		HandshakeFU eastServerFU = new ServerSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(),
				TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER, true,
				exposeInternalControl);
		handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER, eastServerFU);
		// } else { // we have a client here
		HandshakeFU eastClientFU = new ClientSideHandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(),
				TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, false,
				exposeInternalControl, reporter);
		handshakeFUs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, eastClientFU);
		// }

		loadWiringFromFile();

//		OPCUAInputStationMock ism1 = new OPCUAInputStationMock(server1.getServer(), NAMESPACE_URI, "InputStation", actor1, HandshakeProtocol.INPUTSTATION_CAPABILITY_URI);
//		// differentiate in/out
		Thread s1 = new Thread(opcuaBase);
		s1.start();
	}

	private void loadWiringFromFile() {
		String path;
		if (wiringFilePath.equals("")) {
			path = machineName;
		} else {
			path = wiringFilePath;
		}
		Optional<HashMap<String, WiringInfo>> optInfo = WiringUtils.loadWiringInfoFromFileSystem(path);
		optInfo.ifPresent(info -> {
			info.values().stream().filter(wi -> handshakeFUs.containsKey(wi.getLocalCapabilityId())).forEach(wi -> {
				try {
					handshakeFUs.get(wi.getLocalCapabilityId()).provideWiringInfo(wi);
				} catch (Exception e) {
					log.warning("Error applying wiring info " + e.getMessage());
					e.printStackTrace();
				}
			});
		});
	}

	private void writeWiringToFile() {
		HashMap<String, WiringInfo> wiringMap = new HashMap<>();
		for (String key : handshakeFUs.keySet()) {
			if (handshakeFUs.get(key) instanceof ClientSideHandshakeFU) {
				if (((ClientSideHandshakeFU) handshakeFUs.get(key)).getCurrentWiringInfo() == null) {
					continue; // Skip if there is no current wiringInfo available
				}
				wiringMap.put(key, ((ClientSideHandshakeFU) handshakeFUs.get(key)).getCurrentWiringInfo());
			}
		}
		WiringUtils.writeWiringInfoToFileSystem(wiringMap, machineName);
	}

	private void setupOPCUANodeSet(OPCUABase opcuaBase, UaFolderNode ttNode, String path, ActorRef ttActor) {

		UaMethodNode n1 = opcuaBase.createPartialMethodNode(path,
				TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset.toString(), "Requests reset");
		opcuaBase.addMethodNode(ttNode, n1, new Reset(n1, ttActor));
		UaMethodNode n2 = opcuaBase.createPartialMethodNode(path,
				TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Stop.toString(), "Requests stop");
		opcuaBase.addMethodNode(ttNode, n2, new Stop(n2, ttActor));
		UaMethodNode n3 = opcuaBase.createPartialMethodNode(path, TransportModuleCapability.OPCUA_TRANSPORT_REQUEST,
				"Requests transport");
		opcuaBase.addMethodNode(ttNode, n3, new TransportRequest(n3, ttActor));
		status = opcuaBase.generateStringVariableNode(ttNode, path, OPCUABasicMachineBrowsenames.STATE_VAR_NAME,
				BasicMachineStates.UNKNOWN);
	}

	private void setupTurntableCapabilities(OPCUABase opcuaBase, UaFolderNode ttNode, String path) {
		// add capabilities
		UaFolderNode capabilitiesFolder = opcuaBase.generateFolder(ttNode, path,
				new String(OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
		path = path + "/" + OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = opcuaBase.generateFolder(capabilitiesFolder, path, "CAPABILITY",
				OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);
		opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY",
				OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
				new String(TransportModuleCapability.TRANSPORT_CAPABILITY_URI));
		opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY",
				OPCUACapabilitiesAndWiringInfoBrowsenames.ID, new String("DefaultTurntableCapabilityInstance"));
		opcuaBase.generateStringVariableNode(capability1, path + "/CAPABILITY",
				OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
				new String(OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED));
	}

	private void setStatusValue(String newStatus) {
		if (status != null) {
			status.setValue(new DataValue(new Variant(newStatus)));
		}
	}
}
