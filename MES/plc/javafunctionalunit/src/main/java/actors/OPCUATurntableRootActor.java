package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import event.MachineStatusUpdateEvent;
import event.bus.InterMachineEventBus;
import event.bus.SubscriptionClassifier;
import event.bus.WellknownMachinePropertyFields;
import event.capability.WellknownTransportModuleCapability;
import handshake.WiringUtils;
import handshake.methods.Reset;
import handshake.methods.Stop;
import handshake.methods.TransportRequest;
import msg.InternalTransportModuleRequest;
import opcua.HandshakeFU;
import opcua.OPCUABase;
import opcua.OPCUACapabilitiesWellknownBrowsenames;
import opcua.server.BaseOpcUaServer;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import stateMachines.MachineStatus;

import java.util.HashMap;
import java.util.Optional;

public class OPCUATurntableRootActor extends AbstractActor {

	private String machineName = "Turntable";
	static final String NAMESPACE_URI = "urn:factory-in-a-box";
	private HashMap<String, HandshakeFU> handshakeFUs = new HashMap<>();
	private UaVariableNode status = null;
	private ActorRef ttWrapper;
	
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	static public Props props(String machineName) {	    
		return Props.create(OPCUATurntableRootActor.class, () -> new OPCUATurntableRootActor(machineName));
	}
	
	public OPCUATurntableRootActor(String machineName) {
		try {
			this.machineName = machineName;
			init();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Receive createReceive() {
		
		return receiveBuilder()
				.match(WellknownTransportModuleCapability.SimpleMessageTypes.class, req -> {
					if (ttWrapper != null) ttWrapper.tell(req, getSelf());
					} )
				.match(MachineStatusUpdateEvent.class, req -> {
						setStatusValue(req.getStatus().toString());
					})				
				.match(InternalTransportModuleRequest.class, req -> {
					// forward to return response directly into method call back
					if (ttWrapper != null) ttWrapper.forward(req, getContext());
				}).build();		
	}


	
	private void init() throws Exception {
		BaseOpcUaServer server1;
		if (machineName.equalsIgnoreCase("Turntable1"))
			 server1 = new BaseOpcUaServer(3, machineName);
		else 
			server1 = new BaseOpcUaServer(4, machineName);
		OPCUABase opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
		UaFolderNode root = opcuaBase.prepareRootNode();
		UaFolderNode ttNode = opcuaBase.generateFolder(root, machineName, "Turntable_FU");
		String fuPrefix = machineName+"/"+"Turntable_FU";
						
		InterMachineEventBus intraEventBus = new InterMachineEventBus();
		intraEventBus.subscribe(getSelf(), new SubscriptionClassifier("Turntable Module", "*"));
		//ttWrapper = context().actorOf(TransportModuleCoordinatorActor.props(intraEventBus, turntableFU, conveyorFU), "TT1");
		ttWrapper = context().actorOf(TransportModuleCoordinatorActor.props(intraEventBus,
				context().actorOf(TurntableActor.props(intraEventBus, System.out::println)),
				context().actorOf(ConveyorActor.props(intraEventBus, System.out::println))), "TT1");
		ttWrapper.tell(WellknownTransportModuleCapability.SimpleMessageTypes.SubscribeState, getSelf());
		//ttWrapper.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, getSelf());
		
		setupTurntableCapabilities(opcuaBase, ttNode, fuPrefix);
		setupOPCUANodeSet(opcuaBase, ttNode, fuPrefix, ttWrapper);
		
		// there is always a west, south, north, client
		HandshakeFU westFU = new HandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT, false);
		handshakeFUs.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT, 
				westFU);
		HandshakeFU southFU = new HandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT, false);
		handshakeFUs.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT, 
				southFU);
		HandshakeFU northFU = new HandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT, false);
		handshakeFUs.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT, 
				northFU);
		
		if (machineName.equalsIgnoreCase("Turntable1")) { // we have a server here
			HandshakeFU eastFU = new HandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_SERVER, true);
			handshakeFUs.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_SERVER, 
					eastFU);
		} else { // we have a client here
			HandshakeFU eastFU = new HandshakeFU(opcuaBase, ttNode, fuPrefix, ttWrapper, getContext(), WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT, false);
			handshakeFUs.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT, 
					eastFU);
		}
		
		
		loadWiringFromFile();
		
//		OPCUAInputStationMock ism1 = new OPCUAInputStationMock(server1.getServer(), NAMESPACE_URI, "InputStation", actor1, HandshakeProtocol.INPUTSTATION_CAPABILITY_URI);
//		// differentiate in/out
		Thread s1 = new Thread(opcuaBase);
		s1.start();
	}
	
	private void loadWiringFromFile() {
		Optional<HashMap<String, WiringUtils.WiringInfo>> optInfo = WiringUtils.loadWiringInfoFromFileSystem(machineName);
		optInfo.ifPresent(info -> {
			info.values().stream()
				.filter(wi -> handshakeFUs.containsKey(wi.getLocalCapabilityId()))
				.forEach(wi -> {						
					try {
						handshakeFUs.get(wi.getLocalCapabilityId()).provideWiringInfo(wi);
					} catch (Exception e) {
						log.warning("Error applying wiring info "+e.getMessage());
						e.printStackTrace();
					}
				});
		});
	}
	
	private void setupOPCUANodeSet(OPCUABase opcuaBase, UaFolderNode ttNode, String path, ActorRef ttActor) {
		
		UaMethodNode n1 = opcuaBase.createPartialMethodNode(path, WellknownTransportModuleCapability.SimpleMessageTypes.Reset.toString(), "Requests reset");		
		opcuaBase.addMethodNode(ttNode, n1, new Reset(n1, ttActor));
		UaMethodNode n2 = opcuaBase.createPartialMethodNode(path, WellknownTransportModuleCapability.SimpleMessageTypes.Stop.toString(), "Requests stop");		
		opcuaBase.addMethodNode(ttNode, n2, new Stop(n2, ttActor));
		UaMethodNode n3 = opcuaBase.createPartialMethodNode(path, WellknownTransportModuleCapability.TRANSPORT_MODULE_UPCUA_TRANSPORT_REQUEST, "Requests transport");		
		opcuaBase.addMethodNode(ttNode, n3, new TransportRequest(n3, ttActor));
		status = opcuaBase.generateStringVariableNode(ttNode, path, WellknownMachinePropertyFields.STATE_VAR_NAME, MachineStatus.UNKNOWN);
	}
	
	private void setupTurntableCapabilities(OPCUABase opcuaBase, UaFolderNode ttNode, String path) {
		// add capabilities 
		UaFolderNode capabilitiesFolder = opcuaBase.generateFolder(ttNode, path, new String( OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES));
		path = path +"/"+ OPCUACapabilitiesWellknownBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = opcuaBase.generateFolder(capabilitiesFolder, path,
				"CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.CAPABILITY);
		opcuaBase.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.TYPE,
				new String(WellknownTransportModuleCapability.TURNTABLE_CAPABILITY_URI));
		opcuaBase.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.ID,
				new String("DefaultTurntableCapabilityInstance"));
		opcuaBase.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesWellknownBrowsenames.ROLE,
				new String(OPCUACapabilitiesWellknownBrowsenames.ROLE_VALUE_PROVIDED));
	}
	
	private void setStatusValue(String newStatus) {		
		if(status != null) {
			status.setValue(new DataValue(new Variant(newStatus)));
		}
	}
}
