package fiab.opcua.hardwaremock.plotter;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.meta.OPCUACapabilitiesAndWiringInfoBrowsenames;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.plotter.MockMachineWrapper;
import fiab.mes.mockactors.plotter.MockTransportAwareMachineWrapper;
import fiab.opcua.hardwaremock.plotter.methods.PlotRequest;
import fiab.opcua.hardwaremock.plotter.methods.Reset;
import fiab.opcua.hardwaremock.plotter.methods.Stop;
import fiab.opcua.server.NonEncryptionBaseOpcUaServer;
import fiab.opcua.server.OPCUABase;

public class OPCUAPlotterRootActor extends AbstractActor {

	private String machineName = "Plotter";
	static final String NAMESPACE_URI = "urn:factory-in-a-box";	
	private UaVariableNode status = null;
	private ActorRef plotterWrapper;
	private SupportedColors color;
	
	
	static public Props props(String machineName, SupportedColors color) {	    
		return Props.create(OPCUAPlotterRootActor.class, () -> new OPCUAPlotterRootActor(machineName, color));
	}
	
	public OPCUAPlotterRootActor(String machineName, SupportedColors color) {
		try {
			this.machineName = machineName;
			this.color = color;
			init();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Receive createReceive() {
		
		return receiveBuilder()				
				.match(MachineStatusUpdateEvent.class, req -> {
						setStatusValue(req.getStatus().toString());
					})				
				.build();		
	}


	
	private void init() throws Exception {
		NonEncryptionBaseOpcUaServer server1;
		if (machineName.equalsIgnoreCase("Plotter1"))
			 server1 = new NonEncryptionBaseOpcUaServer(5, machineName);
		else if (machineName.equalsIgnoreCase("Plotter2"))
			server1 = new NonEncryptionBaseOpcUaServer(6, machineName);
		else if (machineName.equalsIgnoreCase("Plotter3"))
			server1 = new NonEncryptionBaseOpcUaServer(7, machineName);
		else 
			server1 = new NonEncryptionBaseOpcUaServer(8, machineName);
		
		OPCUABase opcuaBase = new OPCUABase(server1.getServer(), NAMESPACE_URI, machineName);
		UaFolderNode root = opcuaBase.prepareRootNode();
		UaFolderNode ttNode = opcuaBase.generateFolder(root, machineName, "Plotting_FU");
		String fuPrefix = machineName+"/"+"Plotting_FU";
				
		InterMachineEventBus intraEventBus = new InterMachineEventBus();	
		intraEventBus.subscribe(getSelf(), new SubscriptionClassifier("Plotter Module", "*"));		
		plotterWrapper = context().actorOf(MockTransportAwareMachineWrapper.propsForLateHandshakeBinding(intraEventBus), machineName);
		plotterWrapper.tell(MockMachineWrapper.MessageTypes.SubscribeState, getSelf());
		
		HandshakeFU defaultHandshakeFU = new HandshakeFU();
		ActorRef serverSide = defaultHandshakeFU.setupOPCUANodeSet(plotterWrapper, opcuaBase, ttNode, fuPrefix, getContext());
		plotterWrapper.tell(serverSide, getSelf());
		
		
		setupPlotterCapabilities(opcuaBase, ttNode, fuPrefix, color);
		setupOPCUANodeSet(opcuaBase, ttNode, fuPrefix, plotterWrapper);				
					
		Thread s1 = new Thread(opcuaBase);
		s1.start();
	}
	
	
	private void setupOPCUANodeSet(OPCUABase opcuaBase, UaFolderNode ttNode, String path, ActorRef plotterActor) {
		
		UaMethodNode n1 = opcuaBase.createPartialMethodNode(path, MockMachineWrapper.MessageTypes.Reset.toString(), "Requests reset");		
		opcuaBase.addMethodNode(ttNode, n1, new Reset(n1, plotterActor)); 		
		UaMethodNode n2 = opcuaBase.createPartialMethodNode(path, MockMachineWrapper.MessageTypes.Stop.toString(), "Requests stop");		
		opcuaBase.addMethodNode(ttNode, n2, new Stop(n2, plotterActor));
		UaMethodNode n3 = opcuaBase.createPartialMethodNode(path, MockMachineWrapper.MessageTypes.Plot.toString(), "Requests plot");		
		opcuaBase.addMethodNode(ttNode, n3, new PlotRequest(n3, plotterActor));
		status = opcuaBase.generateStringVariableNode(ttNode, path, OPCUABasicMachineBrowsenames.STATE_VAR_NAME, BasicMachineStates.UNKNOWN);	
	}
	
	private void setupPlotterCapabilities(OPCUABase opcuaBase, UaFolderNode ttNode, String path, SupportedColors color) {
		// add capabilities 
		UaFolderNode capabilitiesFolder = opcuaBase.generateFolder(ttNode, path, new String( OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES));
		path = path +"/"+OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITIES;
		UaFolderNode capability1 = opcuaBase.generateFolder(capabilitiesFolder, path,
				"CAPABILITY", OPCUACapabilitiesAndWiringInfoBrowsenames.CAPABILITY);
		opcuaBase.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.TYPE,
				WellknownPlotterCapability.generatePlottingCapabilityURI(color));
		opcuaBase.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.ID,
				"DefaultPlotterCapabilityInstance");
		opcuaBase.generateStringVariableNode(capability1, path+"/CAPABILITY",  OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE,
				OPCUACapabilitiesAndWiringInfoBrowsenames.ROLE_VALUE_PROVIDED);
	}
	
	private void setStatusValue(String newStatus) {		
		if(status != null) {
			status.setValue(new DataValue(new Variant(newStatus)));
		}
	}
}
