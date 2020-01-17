package fiab.mes.shopfloor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.mockactors.MockClientHandshakeActor;
import fiab.mes.mockactors.MockPlottingMachineWrapperDelegate;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.mockactors.MockTransportAwareMachineWrapper;
import fiab.mes.mockactors.TestBasicMachineActorWithTransport;
import fiab.mes.mockactors.iostation.MockIOStationFactory;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper;
import fiab.mes.mockactors.transport.MockTransportModuleWrapperDelegate;
import fiab.mes.mockactors.transport.TestMockTransportModuleActor;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper.LocalEndpointStatus;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

public class DefaultLayout {

	public ActorSystem system;
	public MockIOStationFactory partsIn;
	public MockIOStationFactory partsOut;
	
	public static final boolean engageAutoReload = true;
	public static final boolean disengageAutoReload = false;
	
	public DefaultLayout(ActorSystem system) {
		this.system = system;
	}
	
	public void setupTwoTurntableWith2MachinesAndIO() throws InterruptedException, ExecutionException {
		final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		
		partsIn = MockIOStationFactory.getMockedInputStation(system, eventBusByRef, engageAutoReload, 34);
		partsOut = MockIOStationFactory.getMockedOutputStation(system, eventBusByRef, engageAutoReload, 35);
		// now add to ttWrapper client Handshake actors
		ActorSelection inServer = system.actorSelection("/user/"+partsIn.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/InputStationServerSideHandshakeMock");
		Thread.sleep(500);
		ActorRef inRef = inServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		ActorSelection outServer = system.actorSelection("/user/"+partsOut.model.getActorName()+MockIOStationFactory.WRAPPER_POSTFIX+"/OutputStationServerSideHandshakeMock");
		Thread.sleep(500);
		ActorRef outRef = outServer.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
	
		// Machines for first turnable
		ActorRef handShakeServer31 = setupMachineActor(eventBusByRef, 31, TestBasicMachineActorWithTransport.getColorCapability("Red"), system);
		ActorRef handShakeServer37 = setupMachineActor(eventBusByRef, 37, TestBasicMachineActorWithTransport.getColorCapability("Blue"), system);				
		// setup turntable1
		InterMachineEventBus intraEventBus1 = new InterMachineEventBus();	
		ActorRef ttWrapper1 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus1), "TT1");
		ActorRef westClient1 = system.actorOf(MockClientHandshakeActor.props(ttWrapper1, inRef), WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT);
		ActorRef northClient1 = system.actorOf(MockClientHandshakeActor.props(ttWrapper1, handShakeServer31), WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT);
		ActorRef southClient1 = system.actorOf(MockClientHandshakeActor.props(ttWrapper1, handShakeServer37), WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT);
		boolean autoComplete = true;
		ActorRef eastServer1 = system.actorOf(MockServerHandshakeActor.props(ttWrapper1, autoComplete), WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_SERVER);
		Map<String, LocalEndpointStatus> eps1 = new HashMap<>();

		eps1.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(northClient1, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT));
		eps1.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(southClient1, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT));
		eps1.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient1, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT));
		eps1.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_SERVER, new MockTransportModuleWrapper.LocalServerEndpointStatus(eastServer1, true, WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_SERVER));
		ttWrapper1.tell(new MockTransportModuleWrapper.HandshakeEndpointInfo(eps1), ActorRef.noSender());
		ttWrapper1.tell(MockTransportModuleWrapper.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
		ttWrapper1.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, ActorRef.noSender());
		
		// Machines for second turntable
		ActorRef handShakeServer32 = setupMachineActor(eventBusByRef, 32, TestBasicMachineActorWithTransport.getColorCapability("Yellow"), system);
		ActorRef handShakeServer38 = setupMachineActor(eventBusByRef, 38, TestBasicMachineActorWithTransport.getColorCapability("Green"), system);
		// setup turntable 2
		InterMachineEventBus intraEventBus2 = new InterMachineEventBus();	
		ActorRef ttWrapper2 = system.actorOf(MockTransportModuleWrapper.props(intraEventBus2), "TT2");
		ActorRef eastClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, outRef), WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT);
		ActorRef westClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, eastServer1), WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT+"~2");
		ActorRef northClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, handShakeServer32), WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT+"~2");
		ActorRef southClient2 = system.actorOf(MockClientHandshakeActor.props(ttWrapper2, handShakeServer38), WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT+"~2");
		
		Map<String, LocalEndpointStatus> eps2 = new HashMap<>();
		eps2.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(northClient2, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_NORTH_CLIENT));
		eps2.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(southClient2, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_SOUTH_CLIENT));		
		eps2.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(westClient2, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_WEST_CLIENT));
		eps2.put(WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT, new MockTransportModuleWrapper.LocalClientEndpointStatus(eastClient2, false, WellknownTransportModuleCapability.TRANSPORT_MODULE_EAST_CLIENT));
		ttWrapper2.tell(new MockTransportModuleWrapper.HandshakeEndpointInfo(eps2), ActorRef.noSender());
		ttWrapper2.tell(MockTransportModuleWrapper.SimpleMessageTypes.SubscribeState, ActorRef.noSender());
		ttWrapper2.tell(MockTransportModuleWrapper.SimpleMessageTypes.Reset, ActorRef.noSender());
		
		// setup actual turntable actors:
		setupTurntableActor(ttWrapper1, intraEventBus1, eventBusByRef, 20, system);
		setupTurntableActor(ttWrapper2, intraEventBus2, eventBusByRef, 21, system);
	}

	public static ActorRef setupTurntableActor(ActorRef ttWrapper, InterMachineEventBus intraEventBus, ActorSelection eventBusByRef, int ipid, ActorSystem system) {
		AbstractCapability cap = WellknownTransportModuleCapability.getTurntableCapability();
		Actor modelActor = TestMockTransportModuleActor.getDefaultTransportModuleModelActor(ipid);
		MockTransportModuleWrapperDelegate hal = new MockTransportModuleWrapperDelegate(ttWrapper);
		Position selfPos = new Position(ipid+"");
		HardcodedDefaultTransportRoutingAndMapping env = new HardcodedDefaultTransportRoutingAndMapping();								
		return system.actorOf(BasicTransportModuleActor.props(eventBusByRef, cap, modelActor, hal, selfPos, intraEventBus, new TransportPositionLookup(), env), "TTActor"+ipid);
	}
	
	public static ActorRef setupMachineActor(ActorSelection eventBusByRef, int ipid, AbstractCapability colorCap, ActorSystem system) throws InterruptedException, ExecutionException {
		InterMachineEventBus intraEventBus = new InterMachineEventBus();
		final AbstractCapability cap = TestBasicMachineActorWithTransport.composeInOne(TestBasicMachineActorWithTransport.getPlottingCapability(), colorCap);
		final Actor modelActor = TestBasicMachineActorWithTransport.getDefaultMachineActor(ipid);
		ActorRef machineWrapper = system.actorOf(MockTransportAwareMachineWrapper.props(intraEventBus), "MachineWrapper"+ipid);
		ActorSelection serverSide = system.actorSelection("/user/MachineWrapper"+ipid+"/ServerSideHandshakeMock");
		Thread.sleep(1000);
		ActorRef serverSideRef = serverSide.resolveOne(Duration.ofSeconds(3)).toCompletableFuture().get();
		PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
		ActorRef machine = system.actorOf(BasicMachineActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
		return serverSideRef;
	}
	
}
