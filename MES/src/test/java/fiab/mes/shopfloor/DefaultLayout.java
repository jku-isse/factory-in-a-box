package fiab.mes.shopfloor;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import fiab.core.capabilities.folding.WellknownFoldingCapability;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.handshake.actor.ClientHandshakeActor;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.ServerSideHandshakeActor;
import fiab.machine.foldingstation.VirtualFoldingMachineActor;
import fiab.machine.plotter.VirtualPlotterCoordinatorActor;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.foldingstation.FoldingStationActor;
import fiab.mes.machine.actor.foldingstation.wrapper.FoldingStationWrapperInterface;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.mockactors.foldingstation.MockFoldingStationWrapperDelegate;
import fiab.mes.mockactors.iostation.VirtualIOStationActorFactory;
import fiab.mes.mockactors.plotter.MockPlottingMachineWrapperDelegate;
import fiab.mes.mockactors.transport.CoreModelActorProvider;
import fiab.mes.mockactors.transport.MockTransportModuleWrapperDelegate;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.actor.TransportModuleCoordinatorActor;
import fiab.turntable.conveying.BaseBehaviorConveyorActor;
import fiab.turntable.turning.BaseBehaviorTurntableActor;

public class DefaultLayout {

	public ActorSystem system;
	public ActorSelection eventBusByRef;
	public VirtualIOStationActorFactory partsIn;
	public VirtualIOStationActorFactory partsOut;
	
	
	public static final boolean engageAutoReload = true;
	public static final boolean disengageAutoReload = false;
	
	
	
	boolean ioStationsInitialized = false;
	
	public DefaultLayout(ActorSystem system, boolean doStartupMachineEventBus) {
		this.system = system;
		if (doStartupMachineEventBus) {		
			init();
		} else {
			eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);	
		}
	}
	
	public DefaultLayout(ActorSystem system) {
		this.system = system;
		init();   	
	}
	
	private void init() {
		ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		 
	};
	
	public void setupIOStations(int inStationPortOffset, int outStationPortOffset) {
		partsIn = VirtualIOStationActorFactory.getMockedInputStation(system, eventBusByRef, engageAutoReload, inStationPortOffset);
		partsIn.wrapper.tell(HandshakeCapability.StateOverrideRequests.SetLoaded, ActorRef.noSender()); 
		partsOut = VirtualIOStationActorFactory.getMockedOutputStation(system, eventBusByRef, engageAutoReload, outStationPortOffset);
		partsOut.wrapper.tell(HandshakeCapability.StateOverrideRequests.SetEmpty, ActorRef.noSender()); 
		ioStationsInitialized = true;
	}
	
	public ActorRef setupSingleTurntable(int id, IntraMachineEventBus intraEventBus, Map<String, ActorRef> clientRefs, Set<String> serverRefs) {	
		ActorRef turntableFU = system.actorOf(BaseBehaviorTurntableActor.props(intraEventBus, null), "TT"+id+"-TurntableFU");
		ActorRef conveyorFU = system.actorOf(BaseBehaviorConveyorActor.props(intraEventBus, null), "TT"+id+"-ConveyorFU");
		ActorRef ttWrapper = system.actorOf(TransportModuleCoordinatorActor.props(intraEventBus, turntableFU, conveyorFU), "TT"+id);
		clientRefs.entrySet().stream().forEach(entry -> {
			ActorRef client = system.actorOf(ClientHandshakeActor.props(ttWrapper, entry.getValue()), entry.getKey()+"~"+id); 
			ttWrapper.tell(new LocalEndpointStatus.LocalClientEndpointStatus(client, false, entry.getKey()), ActorRef.noSender());
		});
		serverRefs.stream().forEach(entry -> {
			ActorRef server = system.actorOf(ServerSideHandshakeActor.props(ttWrapper, true), entry+"~"+id); 
			ttWrapper.tell(new LocalEndpointStatus.LocalServerEndpointStatus(server, true, entry), ActorRef.noSender());
		});
		return ttWrapper;
	}
	
	
	public ActorRef setupSingleTT21withIOEast35West20() throws Exception{
		setupIOStations(20, 35);
		ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
		ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
		ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		// setup turntable
		IntraMachineEventBus intraEventBus = new IntraMachineEventBus();	
		Map<String,ActorRef> ioRefs = new HashMap<String,ActorRef>();
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, inRef);
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, outRef);
		ActorRef ttWrapper = setupSingleTurntable(21, intraEventBus, ioRefs, new HashSet<String>());
		return setupMESLevelTurntableActor(ttWrapper, intraEventBus, eventBusByRef, 21);
	}
	
	// Returns list of MES level ActorRefs for TT1 and TT2
	public List<ActorRef> setupDualTT2021withIOEast35West34() throws Exception{
		setupIOStations(34, 35);
		ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
		ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
		ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		// setup turntable
		IntraMachineEventBus intraEventBus = new IntraMachineEventBus();	
		Map<String,ActorRef> iRefs = new HashMap<String,ActorRef>();
		iRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, inRef);
		Set<String> serverRefs = new HashSet<String>();
		serverRefs.add(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER);
		ActorRef ttWrapper = setupSingleTurntable(20, intraEventBus, iRefs, serverRefs);
		ActorRef mesTT1 = setupMESLevelTurntableActor(ttWrapper, intraEventBus, eventBusByRef, 20);
		
		IntraMachineEventBus intraEventBus2 = new IntraMachineEventBus();	
		Map<String,ActorRef> ioRefs = new HashMap<String,ActorRef>();
		ActorSelection eastServerSel = system.actorSelection("/user/"+TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER+"~"+20);
		ActorRef eastServer = eastServerSel.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, outRef);
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, eastServer);
		ActorRef ttWrapper2 = setupSingleTurntable(21, intraEventBus2, ioRefs, new HashSet<String>());
		ActorRef mesTT2 = setupMESLevelTurntableActor(ttWrapper2, intraEventBus2, eventBusByRef, 21);
		
		return Arrays.asList(new ActorRef[]{mesTT1,mesTT2});
	}
	
	public List<ActorRef> setupTwoTurntableWith2MachinesAndIO() throws Exception{
		//IO Stations
		setupIOStations(34, 35);
		ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
		ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
		ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		//Plotters
		ActorRef handShakeServer31 = setupMachineActor(eventBusByRef, 31, WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.RED), system);
		ActorRef handShakeServer37 = setupMachineActor(eventBusByRef, 37, WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.BLUE), system);	
		ActorRef handShakeServer32 = setupMachineActor(eventBusByRef, 32, WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.BLACK), system);
		ActorRef handShakeServer38 = setupMachineActor(eventBusByRef, 38, WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.GREEN), system);	
		
		// setup turntable
		IntraMachineEventBus intraEventBus = new IntraMachineEventBus();	
		Map<String,ActorRef> iRefs = new HashMap<String,ActorRef>();
		iRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, inRef);
		iRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, handShakeServer31);
		iRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT, handShakeServer37);
		Set<String> serverRefs = new HashSet<String>();
		serverRefs.add(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER);
		ActorRef ttWrapper = setupSingleTurntable(20, intraEventBus, iRefs, serverRefs);
		ActorRef mesTT1 = setupMESLevelTurntableActor(ttWrapper, intraEventBus, eventBusByRef, 20);
		
		
		
		IntraMachineEventBus intraEventBus2 = new IntraMachineEventBus();	
		Map<String,ActorRef> ioRefs = new HashMap<String,ActorRef>();
		ActorSelection eastServerSel = system.actorSelection("/user/"+TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER+"~"+20);
		ActorRef eastServer = eastServerSel.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, outRef);
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, handShakeServer32);
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT, handShakeServer38);
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, eastServer);
		ActorRef ttWrapper2 = setupSingleTurntable(21, intraEventBus2, ioRefs, new HashSet<String>());
		ActorRef mesTT2 = setupMESLevelTurntableActor(ttWrapper2, intraEventBus2, eventBusByRef, 21);
		
		return Arrays.asList(new ActorRef[]{mesTT1,mesTT2});
	}

	public List<ActorRef> setupTwoTurntableWith2FoldingStationsAndIO() throws Exception{
		//IO Stations
		setupIOStations(34, 35);
		ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
		ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX);
		ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		//Folding Stations TODO
		ActorRef handShakeServer31 = setupFoldingStationActor(eventBusByRef, 31, WellknownFoldingCapability.getFoldingShapeCapability(), system);
		ActorRef handShakeServer37 = setupFoldingStationActor(eventBusByRef, 37, WellknownFoldingCapability.getFoldingShapeCapability(), system);
		ActorRef handShakeServer32 = setupFoldingStationActor(eventBusByRef, 32, WellknownFoldingCapability.getFoldingShapeCapability(), system);
		ActorRef handShakeServer38 = setupFoldingStationActor(eventBusByRef, 38, WellknownFoldingCapability.getFoldingShapeCapability(), system);

		// setup turntable
		IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
		Map<String,ActorRef> iRefs = new HashMap<String,ActorRef>();
		iRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, inRef);
		iRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, handShakeServer31);
		iRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT, handShakeServer37);
		Set<String> serverRefs = new HashSet<String>();
		serverRefs.add(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER);
		ActorRef ttWrapper = setupSingleTurntable(20, intraEventBus, iRefs, serverRefs);
		ActorRef mesTT1 = setupMESLevelTurntableActor(ttWrapper, intraEventBus, eventBusByRef, 20);

		IntraMachineEventBus intraEventBus2 = new IntraMachineEventBus();
		Map<String,ActorRef> ioRefs = new HashMap<String,ActorRef>();
		ActorSelection eastServerSel = system.actorSelection("/user/"+TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER+"~"+20);
		ActorRef eastServer = eastServerSel.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, outRef);
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, handShakeServer32);
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT, handShakeServer38);
		ioRefs.put(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, eastServer);
		ActorRef ttWrapper2 = setupSingleTurntable(21, intraEventBus2, ioRefs, new HashSet<String>());
		ActorRef mesTT2 = setupMESLevelTurntableActor(ttWrapper2, intraEventBus2, eventBusByRef, 21);

		return Arrays.asList(mesTT1,mesTT2);
	}
	
//	public void setupTwoTurntableWith2MachinesAndIO() throws InterruptedException, ExecutionException {
//		final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
//		
//		partsIn = VirtualIOStationActorFactory.getMockedInputStation(system, eventBusByRef, engageAutoReload, 34);
//		partsOut = VirtualIOStationActorFactory.getMockedOutputStation(system, eventBusByRef, engageAutoReload, 35);
//		// now add to ttWrapper client Handshake actors
//		ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
//		Thread.sleep(500);
//		ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
//		ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+VirtualIOStationActorFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
//		Thread.sleep(500);
//		ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
//	
//		// Machines for first turnable
//		ActorRef handShakeServer31 = setupMachineActor(eventBusByRef, 31, WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.RED), system);
//		ActorRef handShakeServer37 = setupMachineActor(eventBusByRef, 37, WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.BLUE), system);				
//		// setup turntable1
//		InterMachineEventBus intraEventBus1 = new InterMachineEventBus();	
//		ActorRef ttWrapper1 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus1), "TT1");
//		ActorRef westClient1 = system.actorOf(ClientHandshakeActor.props(ttWrapper1, inRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT);
//		ActorRef northClient1 = system.actorOf(ClientHandshakeActor.props(ttWrapper1, handShakeServer31), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT);
//		ActorRef southClient1 = system.actorOf(ClientHandshakeActor.props(ttWrapper1, handShakeServer37), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT);
//		boolean autoComplete = true;
//		ActorRef eastServer1 = system.actorOf(ServerSideHandshakeActor.props(ttWrapper1, autoComplete), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER);
//		//Map<String, LocalEndpointStatus> eps1 = new HashMap<>();
//
//		ttWrapper1.tell( new LocalEndpointStatus.LocalClientEndpointStatus(northClient1, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT), ActorRef.noSender());
//		ttWrapper1.tell( new LocalEndpointStatus.LocalClientEndpointStatus(southClient1, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT), ActorRef.noSender());
//		ttWrapper1.tell( new LocalEndpointStatus.LocalClientEndpointStatus(westClient1, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT), ActorRef.noSender());
//		ttWrapper1.tell( new LocalEndpointStatus.LocalServerEndpointStatus(eastServer1, true, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER), ActorRef.noSender());
//		ttWrapper1.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
//		ttWrapper1.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
//		
//		// Machines for second turntable
//		ActorRef handShakeServer32 = setupMachineActor(eventBusByRef, 32, WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.BLACK), system);
//		ActorRef handShakeServer38 = setupMachineActor(eventBusByRef, 38, WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.GREEN), system);
//		// setup turntable 2
//		InterMachineEventBus intraEventBus2 = new InterMachineEventBus();	
//		ActorRef ttWrapper2 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus2), "TT2");
//		ActorRef eastClient2 = system.actorOf(ClientHandshakeActor.props(ttWrapper2, outRef), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT);
//		ActorRef westClient2 = system.actorOf(ClientHandshakeActor.props(ttWrapper2, eastServer1), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT+"~2");
//		ActorRef northClient2 = system.actorOf(ClientHandshakeActor.props(ttWrapper2, handShakeServer32), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT+"~2");
//		ActorRef southClient2 = system.actorOf(ClientHandshakeActor.props(ttWrapper2, handShakeServer38), TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT+"~2");
//		
//		//Map<String, LocalEndpointStatus> eps2 = new HashMap<>();
//		ttWrapper2.tell( new LocalEndpointStatus.LocalClientEndpointStatus(northClient2, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT), ActorRef.noSender());
//		ttWrapper2.tell( new LocalEndpointStatus.LocalClientEndpointStatus(southClient2, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_CLIENT), ActorRef.noSender());		
//		ttWrapper2.tell( new LocalEndpointStatus.LocalClientEndpointStatus(westClient2, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT), ActorRef.noSender());
//		ttWrapper2.tell( new LocalEndpointStatus.LocalClientEndpointStatus(eastClient2, false, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT), ActorRef.noSender());
//		ttWrapper2.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
//		ttWrapper2.tell(TurntableModuleWellknownCapabilityIdentifiers.SimpleMessageTypes.Reset, ActorRef.noSender());
//		
//		// setup actual turntable actors:
//		setupTurntableActor(ttWrapper1, intraEventBus1, eventBusByRef, 20, system);
//		setupTurntableActor(ttWrapper2, intraEventBus2, eventBusByRef, 21, system);
//	}
//	

	
	private ActorRef setupMESLevelTurntableActor(ActorRef ttWrapper, IntraMachineEventBus intraEventBus, ActorSelection eventBusByRef, int ipid ) {
		AbstractCapability cap = TransportModuleCapability.getTransportCapability();
		Actor modelActor = CoreModelActorProvider.getDefaultTransportModuleModelActor(ipid, 0);
		MockTransportModuleWrapperDelegate hal = new MockTransportModuleWrapperDelegate(ttWrapper);
		Position selfPos = new Position(ipid+"");
		HardcodedDefaultTransportRoutingAndMapping env = new HardcodedDefaultTransportRoutingAndMapping();								
		return system.actorOf(BasicTransportModuleActor.props(eventBusByRef, cap, modelActor, hal, selfPos, intraEventBus, new DefaultTransportPositionLookup(), env), "MESlevelTTActor"+ipid);
	}
	
	// Returns machine-level ActorRef of the ServerSideHandshakeActor
	public static ActorRef setupMachineActor(ActorSelection eventBusByRef, int ipid, AbstractCapability colorCap, ActorSystem system) throws InterruptedException, ExecutionException {
		fiab.machine.plotter.IntraMachineEventBus intraEventBus = new fiab.machine.plotter.IntraMachineEventBus();
		final AbstractCapability cap = colorCap;
		final Actor modelActor = getDefaultMachineActor(ipid);
		ActorRef machineWrapper = system.actorOf(VirtualPlotterCoordinatorActor.props(intraEventBus), "MachineWrapper"+ipid);
		ActorSelection serverSide = system.actorSelection("/user/MachineWrapper"+ipid+"/ServerSideHandshakeMock");
		Thread.sleep(1000);
		ActorRef serverSideRef = serverSide.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
		ActorRef machine = system.actorOf(BasicMachineActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
		return serverSideRef;
	}

	// Returns machine-level ActorRef of the ServerSideHandshakeActor
	public static ActorRef setupFoldingStationActor(ActorSelection eventBusByRef, int ipid, AbstractCapability foldingCap, ActorSystem system) throws InterruptedException, ExecutionException {
		fiab.machine.foldingstation.IntraMachineEventBus intraEventBus = new fiab.machine.foldingstation.IntraMachineEventBus();
		final AbstractCapability cap = foldingCap;
		final Actor modelActor = getDefaultMachineActor(ipid);
		ActorRef machineWrapper = system.actorOf(VirtualFoldingMachineActor.props(intraEventBus), "StationWrapper"+ipid);
		ActorSelection serverSide = system.actorSelection("/user/StationWrapper"+ipid+"/ServerSideHandshakeMock");
		Thread.sleep(1000);
		ActorRef serverSideRef = serverSide.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		FoldingStationWrapperInterface wrapperDelegate = new MockFoldingStationWrapperDelegate(machineWrapper);
		ActorRef machine = system.actorOf(FoldingStationActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
		return serverSideRef;
	}

	public static Actor getDefaultMachineActor(int id) {
		Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
		actor.setID("MockMachineActor"+id);
		actor.setActorName("MockMachineActor"+id);
		actor.setDisplayName("MockMachineActor"+id);
		actor.setUri("http://192.168.0."+id+"/MockMachineActor"+id);
		return actor;
	}
}
