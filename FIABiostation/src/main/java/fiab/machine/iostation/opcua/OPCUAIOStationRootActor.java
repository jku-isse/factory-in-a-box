package fiab.machine.iostation.opcua;

import java.time.Duration;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.fu.HandshakeFU;
import fiab.opcua.CapabilityExposingUtils;
import fiab.opcua.CapabilityImplementationMetadata;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.opcua.server.NonEncryptionBaseOpcUaServer;
import fiab.opcua.server.PublicNonEncryptionBaseOpcUaServer;
import fiab.opcua.server.OPCUABase;

public class OPCUAIOStationRootActor extends AbstractActor {
	
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	private String machineName = "IO";
	static final String NAMESPACE_URI = "urn:factory-in-a-box";	
	private UaVariableNode statusIOS = null;
	private HandshakeFU fu;
	protected ActorRef self;
	boolean isInputStation;
	boolean doAutoReload;
	private final boolean useRealHardware;
	
	static public Props propsForInputStation(String machineName, int portOffset, boolean doAutoReload, boolean useRealHardware) {
		return Props.create(OPCUAIOStationRootActor.class, () -> new OPCUAIOStationRootActor(machineName, portOffset, true, doAutoReload, useRealHardware));
	}
	
	static public Props propsForOutputStation(String machineName, int portOffset, boolean doAutoReload, boolean useRealHardware) {
		return Props.create(OPCUAIOStationRootActor.class, () -> new OPCUAIOStationRootActor(machineName, portOffset, false, doAutoReload, useRealHardware));
	}

	public OPCUAIOStationRootActor(String machineName, int portOffset, boolean isInputStation, boolean doAutoReload, boolean useRealHardware) {
		try {
			this.machineName = machineName;
			this.isInputStation = isInputStation;
			this.doAutoReload = doAutoReload;
			this.useRealHardware = useRealHardware;
			this.self = self();
			init(portOffset);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()				
				.match(ServerSideStates.class, req -> {
						setStatusValue(req.toString());
						if (req.equals(ServerSideStates.COMPLETE) && doAutoReload) { //we auto reload here
							reloadPallet();
						}
					})
				.match(LocalEndpointStatus.LocalServerEndpointStatus.class, les -> {//ignore
					})
				.build();		
	}


	
	private void init(int portOffset) throws Exception {
		PublicNonEncryptionBaseOpcUaServer server1 = new PublicNonEncryptionBaseOpcUaServer(portOffset, machineName);
		OPCUABase opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
		UaFolderNode root = opcuaBase.prepareRootNode();
		String fuPrefix = machineName;//+"/"+"IOSTATION";
		if(useRealHardware){
			fu = isInputStation ? new IOStationHandshakeFU.EV3InputStationHandshakeFU(opcuaBase, root, fuPrefix, getSelf(), getContext(), "DefaultServerSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, true) :
					new IOStationHandshakeFU.EV3OutputStationHandshakeFU(opcuaBase, root, fuPrefix, getSelf(), getContext(), "DefaultServerSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, true);
		}else {
			fu = isInputStation ? new IOStationHandshakeFU.InputStationHandshakeFU(opcuaBase, root, fuPrefix, getSelf(), getContext(), "DefaultServerSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, true) :
					new IOStationHandshakeFU.OutputStationHandshakeFU(opcuaBase, root, fuPrefix, getSelf(), getContext(), "DefaultServerSideHandshake", OPCUACapabilitiesAndWiringInfoBrowsenames.IS_PROVIDED, true);
		}
		setupOPCUANodeSet(opcuaBase, root, fuPrefix, fu.getFUActor());
		CapabilityExposingUtils.setupCapabilities(opcuaBase, root, fuPrefix, new CapabilityImplementationMetadata("DefaultStation",
										isInputStation ? IOStationCapability.INPUTSTATION_CAPABILITY_URI : IOStationCapability.OUTPUTSTATION_CAPABILITY_URI, 
										ProvOrReq.PROVIDED));
		Thread s1 = new Thread(opcuaBase);
		s1.start();
		if (doAutoReload) {
			reloadPallet();
		}
	}
	
	
	private void setupOPCUANodeSet(OPCUABase opcuaBase, UaFolderNode folderNode, String path, ActorRef actor) {
		
		UaMethodNode n1 = opcuaBase.createPartialMethodNode(path, IOStationCapability.RESET_REQUEST, "Requests reset");		
		opcuaBase.addMethodNode(folderNode, n1, new fiab.handshake.fu.server.methods.Reset(n1, actor)); 		
		UaMethodNode n2 = opcuaBase.createPartialMethodNode(path, IOStationCapability.STOP_REQUEST, "Requests stop");		
		opcuaBase.addMethodNode(folderNode, n2, new fiab.handshake.fu.server.methods.Stop(n2, actor));
		//if (IOStationCapability.STATE_VAR_NAME != IOStationCapability.OPCUA_STATE_SERVERSIDE_VAR_NAME) {
			statusIOS = opcuaBase.generateStringVariableNode(folderNode, path, IOStationCapability.STATE_VAR_NAME, ServerSideStates.STOPPED);
		//}
	}

	public void setStatusValue(String newStatus) {
		if(statusIOS != null) {
			statusIOS.setValue(new DataValue(new Variant(newStatus)));
		}
	}
	
	private void reloadPallet() {
		//tell handshake that the pallet is loaded if inputstation, otherwise setempty
			log.info(self.path().name()+": Auto Reloading Pallet");
			context().system()
	    	.scheduler()
	    	.scheduleOnce(Duration.ofMillis(1000), 
	    			 new Runnable() {
	            @Override
	            public void run() {
	            		if (isInputStation) {
	        				fu.getFUActor().tell(HandshakeCapability.StateOverrideRequests.SetLoaded, self); 
	        			} else {
	        				fu.getFUActor().tell(HandshakeCapability.StateOverrideRequests.SetEmpty, self); 
	        			}
	            }
	          }, context().system().dispatcher());
	}
	
}
