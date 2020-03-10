package fiab.mes.mockactors.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.mockactors.MockClientHandshakeActor;
import fiab.mes.mockactors.MockServerHandshakeActor;
import fiab.mes.mockactors.iostation.MockIOStationFactory;
import fiab.mes.mockactors.oldplotter.TestMockMachineActor;
import fiab.mes.mockactors.plotter.MockPlottingMachineWrapperDelegate;
import fiab.mes.mockactors.plotter.MockTransportAwareMachineWrapper;
import fiab.mes.mockactors.plotter.TestBasicMachineActorWithTransport;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper;
import fiab.mes.mockactors.transport.MockTransportModuleWrapperDelegate;
import fiab.mes.mockactors.transport.TestMockTransportModuleActor;
import fiab.mes.mockactors.transport.MockTransportModuleWrapper.LocalEndpointStatus;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.shopfloor.DefaultLayout;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportmodule.WellknownTransportModuleCapability;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.RegisterTransportRequest;
import fiab.mes.transport.msg.RegisterTransportRequestStatusResponse;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerMessageTypes;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.order.msg.OrderEvent.OrderEventType;

class TestMachineAndTransportSystem {

	private static final Logger logger = LoggerFactory.getLogger(TestMachineAndTransportSystem.class);
	
	protected static ActorSystem system;
	public static String ROOT_SYSTEM = "routes";
	protected static ActorRef machineEventBus;
	protected static ActorRef orderEventBus;
	protected static ActorRef coordActor;
	HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
	TransportPositionLookup dns = new TransportPositionLookup();
	static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
	
	
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		// setup shopfloor
		// setup machines				
		system = ActorSystem.create(ROOT_SYSTEM);
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderEventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		
	}

	@Before
	public static void setupBeforeEach() {
		knownActors.clear();
	}
	
	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}
	
	@Test
	void testTransport34InTo31Mto35Out() throws InterruptedException, ExecutionException {
		new TestKit(system) { 
			{ 															
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );

				coordActor = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns), "TransportCoordinator");				
				new DefaultLayout(system).setupTwoTurntableWith2MachinesAndIO();
				boolean machineReady = false;
				int countConnEvents = 0;
				while (countConnEvents < 8) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
						//System.out.println("Connected Event: "+countConnEvents);
					}
					if (te instanceof MachineStatusUpdateEvent && !machineReady) {
			fail("LockForRequest requires ResisterRequest first"); //FIXME:
						machineReady = lockMachineforRequest((MachineStatusUpdateEvent) te, "MockMachineActor31", getRef());
					}
				}
				assert(dns.getActorForPosition(new Position("20")).get().equals(knownActors.get("MockTurntableActor20")));
				// now bring one machine into ready state if not already done
				while (!machineReady) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent) {
			fail("LockForRequest requires ResisterRequest first"); //FIXME:
						machineReady = lockMachineforRequest((MachineStatusUpdateEvent) te, "MockMachineActor31", getRef());
					}
				}
				// once ready and assigned, then trigger transport request
				RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get("InputStationActor34"), knownActors.get("MockMachineActor31"), "TestOrder1", getRef());
				coordActor.tell(rtr, getRef());
				boolean transportDone = false;
				while(!transportDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class);
					logEvent(te);
					if (te instanceof RegisterTransportRequestStatusResponse) {
						RegisterTransportRequestStatusResponse rtrr = (RegisterTransportRequestStatusResponse) te;
						if (rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.COMPLETED)) {
							transportDone = true;
						} else { 
							assertTrue(rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.QUEUED) ||
									rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.ISSUED)) ;
						}
					}
					
				}
//				// then unload machine once machine is done
				boolean machineDone = false;
				transportDone = false;
				while (!machineDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class);
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent) {
						machineDone = unloadMachineforRequest((MachineStatusUpdateEvent) te, "MockMachineActor31", getRef());
					} 
				}
				// and we wait for unloading to complete
				while(!transportDone) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class, IOStationStatusUpdateEvent.class, RegisterTransportRequestStatusResponse.class);
					logEvent(te);
					if (te instanceof RegisterTransportRequestStatusResponse) {
						RegisterTransportRequestStatusResponse rtrr = (RegisterTransportRequestStatusResponse) te;
						if (rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.COMPLETED)) {
							transportDone = true;
						} else { 
							assertTrue(rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.QUEUED) ||
									rtrr.getResponse().equals(RegisterTransportRequestStatusResponse.ResponseType.ISSUED)) ;
						}
					}
					
				}
			}	
		};
	}
	
	
	
	
	private boolean lockMachineforRequest(MachineStatusUpdateEvent mue, String matchMachineId, ActorRef testSys) {
		MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
		if (newState.equals(MachineStatus.IDLE) && mue.getMachineId().equals(matchMachineId)) {
			ActorRef machine = knownActors.get(matchMachineId).getAkkaActor();
			logger.info("Sending lock for Order request to machine");
			machine.tell(new LockForOrder("TestStep1","TestRootOrder1"), testSys); // here we dont register and wait for readyness, wont work later
			return true;
		} 
		return false;
	}
	
	private boolean unloadMachineforRequest(MachineStatusUpdateEvent mue, String matchMachineId, ActorRef testSys) {
		MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
		if (newState.equals(MachineStatus.COMPLETING) && mue.getMachineId().equals(matchMachineId)) {
			RegisterTransportRequest rtr = new RegisterTransportRequest(knownActors.get(matchMachineId), knownActors.get("OutputStationActor35"), "TestOrder2", testSys);
			coordActor.tell(rtr, testSys);
			return true;
		} 
		return false;
	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
		if (event instanceof MachineConnectedEvent) {
			knownActors.put(((MachineConnectedEvent) event).getMachineId(), ((MachineConnectedEvent) event).getMachine());
		}
	}
	

}
