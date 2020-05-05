package fiab.mes.mockactors.plotter;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import ProcessCore.ProcessStep;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.machine.plotter.IntraMachineEventBus;
import fiab.machine.plotter.SubscriptionClassifier;
import fiab.machine.plotter.VirtualPlotterCoordinatorActor;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.shopfloor.DefaultLayout;

public class TestBasicMachineActorWithTransport { 

	protected static ActorSystem system;
	protected static ActorRef machine;
	public static String ROOT_SYSTEM = "routes";
	protected OrderProcess op;
	
	private static final Logger logger = LoggerFactory.getLogger(TestBasicMachineActorWithTransport.class);
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		
		system = ActorSystem.create(ROOT_SYSTEM);
		ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	}

	@BeforeEach
	void setUp() throws Exception {
		ProcessCore.Process p = ProduceProcess.getSequential4ColorProcess("P1-");
		op = new OrderProcess(p);
		op.activateProcess();
	}
	
	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test
	void testPlotAtMachineWithServerSideAutoComplete() {
		final AbstractCapability cap = WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.BLACK);
		final Actor modelActor = DefaultLayout.getDefaultMachineActor(1);
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				//eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				
				IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				ActorRef machineWrapper = system.actorOf(VirtualPlotterCoordinatorActor.props(intraEventBus), "MachineWrapper1");
				ActorSelection serverSide = system.actorSelection("/user/MachineWrapper1/ServerSideHandshakeMock");
				PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
				machine = system.actorOf(BasicMachineActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
				// we subscribe to the intraeventbus to observe wrapper behavior
				ProcessStep step = op.getAvailableSteps().get(0);
				boolean doRun = true;
				boolean sentReq = false;
				while (doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, ReadyForProcessEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(BasicMachineStates.STOPPED)) { 							
							machine.tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						} else if (msue.getStatus().equals(BasicMachineStates.IDLE) && !sentReq) { 							
							machine.tell(new RegisterProcessStepRequest("Order1", step.getID(), step, getRef()), getRef());
							sentReq = true;
						} else if (msue.getStatus().equals(BasicMachineStates.STARTING)) {
							boolean handshakeDone = false;
							serverSide.tell(IOStationCapability.ServerMessageTypes.SubscribeToStateUpdates, getRef());
							while (!handshakeDone) {
								ServerSideStates state = expectMsgClass(Duration.ofSeconds(5), ServerSideStates.class);
								switch(state) {
								case IDLE_EMPTY:
									serverSide.tell(IOStationCapability.ServerMessageTypes.RequestInitiateHandover, getRef());
									expectMsg(Duration.ofSeconds(5), ServerSideStates.STARTING);
									expectMsg(Duration.ofSeconds(5), IOStationCapability.ServerMessageTypes.OkResponseInitHandover);
									break;
								case READY_EMPTY:
									serverSide.tell(IOStationCapability.ServerMessageTypes.RequestStartHandover, getRef());
									expectMsg(Duration.ofSeconds(5), ServerSideStates.EXECUTE);
									expectMsg(Duration.ofSeconds(5), IOStationCapability.ServerMessageTypes.OkResponseStartHandover);
									serverSide.tell(IOStationCapability.ServerMessageTypes.UnsubscribeToStateUpdates, getRef()); //otherwise the handshake events interfere with other expected events
									handshakeDone = true; // part until where we need to be involved, thanks to autocomplete
									break;
								default:
									break;
								}
							}
						} 
						if (msue.getStatus().equals(BasicMachineStates.COMPLETING)) {
							doRun = false;
						}
					}
					if (te instanceof ReadyForProcessEvent) {
						assert(((ReadyForProcessEvent) te).isReady());
						machine.tell(new LockForOrder(step.toString(), "Order1"), getRef());						
					}

				}			
				
				
//				boolean doRun = true;
//				while (doRun) {
//					MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);
//					logEvent(mue);
//					MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
//					if (newState.equals(MachineStatus.IDLE)) {
//						machine.tell(new LockForOrder("TestStep1","TestRootOrder1"), getRef()); // here we dont register and wait for readyness, wont work later
//					}
//					if (newState.equals(MachineStatus.STARTING)) {
//						boolean handshakeDone = false;
//						serverSide.tell(HandshakeProtocol.ServerMessageTypes.SubscribeToStateUpdates, getRef());
//						while (!handshakeDone) {
//							ServerSide state = expectMsgClass(Duration.ofSeconds(5), ServerSide.class);
//							switch(state) {
//							case IDLE_EMPTY:
//								serverSide.tell(HandshakeProtocol.ServerMessageTypes.RequestInitiateHandover, getRef());
//								expectMsg(Duration.ofSeconds(5), ServerSide.STARTING);
//								expectMsg(Duration.ofSeconds(5), HandshakeProtocol.ServerMessageTypes.OkResponseInitHandover);
//								break;
//							case READY_EMPTY:
//								serverSide.tell(HandshakeProtocol.ServerMessageTypes.RequestStartHandover, getRef());
//								expectMsg(Duration.ofSeconds(5), ServerSide.EXECUTE);
//								expectMsg(Duration.ofSeconds(5), HandshakeProtocol.ServerMessageTypes.OkResponseStartHandover);
//								serverSide.tell(HandshakeProtocol.ServerMessageTypes.UnsubscribeToStateUpdates, getRef()); //otherwise the handshake events interfere with other expected events
//								handshakeDone = true; // part until where we need to be involved, thanks to autocomplete
//								break;
//							default:
//								break;
//							}
//						}
//					}
//					if (newState.equals(MachineStatus.COMPLETING)) {
//						doRun = false;
//					}
//				}
				//expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
			}	
		};
	}
		
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}
	
//	@Test
//	void testRegisterProcess() {
//		final AbstractCapability cap = WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.RED);
//		final Actor modelActor = getDefaultMachineActor(1);
//		new TestKit(system) { 
//			{
//				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
//				//eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
//				
//				
//				InterMachineEventBus intraEventBus = new InterMachineEventBus();
//				ActorRef machineWrapper = system.actorOf(MockMachineWrapper.props(intraEventBus));
//				PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
//				machine = system.actorOf(BasicMachineActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
//				// we subscribe to the intraeventbus to observe wrapper behavior
//				boolean isIdle = false;
//				while (!isIdle) {
//					TimedEvent te = expectMsgClass(Duration.ofSeconds(3600), TimedEvent.class);
//					logEvent(te);
//				}
//				//expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
//				ProcessStep step = op.getAvailableSteps().get(0);
//				RegisterProcessStepRequest req = new RegisterProcessStepRequest("Order1", step.toString(), step, getRef());
//				machine.tell(req, getRef());
//				expectMsgAnyClassOf(Duration.ofSeconds(5), ReadyForProcessEvent.class);
//			}	
//		};
//	}
		
	
//	@Test
//	void testLockForOrder() {
//		final AbstractCapability cap = WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.RED);
//		final Actor modelActor = getDefaultMachineActor(1);
//		new TestKit(system) { 
//			{
//				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
//				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
//				machine = system.actorOf(MockMachineActor.props(eventBusByRef, cap, modelActor));
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
//				ProcessStep step = op.getAvailableSteps().get(0);
//				RegisterProcessStepRequest req = new RegisterProcessStepRequest("Order1", step.toString(), step, getRef());
//				machine.tell(req, getRef());
//				expectMsgAnyClassOf(Duration.ofSeconds(3), ReadyForProcessEvent.class);
//				machine.tell(new LockForOrder(step.toString(), "Order1"), getRef());
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); // producting
//				expectMsgAnyClassOf(Duration.ofSeconds(7), MachineUpdateEvent.class); // completing
//				expectMsgAnyClassOf(Duration.ofSeconds(2), MachineUpdateEvent.class); // idle
//			}	
//		};
//	}

	

	

	
	
}
