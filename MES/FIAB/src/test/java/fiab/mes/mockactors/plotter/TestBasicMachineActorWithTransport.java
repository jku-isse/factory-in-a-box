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
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
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
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				
				IntraMachineEventBus intraEventBus = new IntraMachineEventBus();
				//intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
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
				
			}	
		};
	}
		
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}
	


	

	

	
	
}
