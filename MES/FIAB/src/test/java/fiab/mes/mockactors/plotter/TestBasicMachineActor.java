package fiab.mes.mockactors.plotter;

import java.time.Duration;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.ProcessStep;
import ProcessCore.XmlRoot;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.mes.capabilities.plotting.EcoreProcessUtils;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.oldplotter.MockMachineActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;

public class TestBasicMachineActor { 

	protected static ActorSystem system;
	protected static ActorRef machine;
	public static String ROOT_SYSTEM = "routes";
	protected OrderProcess op;
	
	private static final Logger logger = LoggerFactory.getLogger(TestBasicMachineActor.class);
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		
		system = ActorSystem.create(ROOT_SYSTEM);
		ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	}

	@BeforeEach
	void setUp() throws Exception {
		ProcessCore.Process p = fiab.mes.order.ecore.ProduceProcess.getSequential4ColorProcess("P1-");
		op = new OrderProcess(p);
		op.activateProcess();
	}
	
	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test
	void testStartMachine() {
		final AbstractCapability cap = WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.RED);
		final Actor modelActor = getDefaultMachineActor(1);
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				//eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				
				InterMachineEventBus intraEventBus = new InterMachineEventBus();
				ActorRef machineWrapper = system.actorOf(MockMachineWrapper.props(intraEventBus));
				PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
				machine = system.actorOf(BasicMachineActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
				// we subscribe to the intraeventbus to observe wrapper behavior
				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				
				boolean isIdle = false;
				while (!isIdle) {
					MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);
					logEvent(mue);
					MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
					if (newState.equals(MachineStatus.IDLE))
						isIdle = true;
				}
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
//				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
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
		
	
	@Test
	void testLockForOrder() {
		final AbstractCapability cap = WellknownPlotterCapability.getColorPlottingCapability(SupportedColors.BLACK);
		final Actor modelActor = getDefaultMachineActor(1);
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				InterMachineEventBus intraEventBus = new InterMachineEventBus();
				ActorRef machineWrapper = system.actorOf(MockMachineWrapper.props(intraEventBus));
				PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
				machine = system.actorOf(BasicMachineActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
				
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();				
				ProcessStep step = op.getAvailableSteps().get(0);
				String machineId = "";
				boolean doRun = true;
				int countConnEvents = 0;
				boolean sentReq = false;
				while (countConnEvents < 1 || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, ReadyForProcessEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
						machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
						machineId = ((MachineConnectedEvent) te).getMachineId();
					}
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(MachineStatus.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						} else if (msue.getStatus().equals(MachineStatus.IDLE) && !sentReq) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new RegisterProcessStepRequest("Order1", step.getID(), step, getRef()), getRef());
							sentReq = true;
						} else if (msue.getStatus().equals(MachineStatus.STARTING)) {
							doRun = false;
						}
					}
					if (te instanceof ReadyForProcessEvent) {
						assert(((ReadyForProcessEvent) te).isReady());
						machines.get(machineId).getAkkaActor().tell(new LockForOrder(step.toString(), "Order1"), getRef());						
					}

				}																
//				expectMsgAnyClassOf(Duration.ofSeconds(3), MachineConnectedEvent.class);
//				expectMsgAnyClassOf(Duration.ofSeconds(3), MachineUpdateEvent.class); //Stopped
//				
//				
//				ProcessStep step = op.getAvailableSteps().get(0);
//				RegisterProcessStepRequest req = new RegisterProcessStepRequest("Order1", step.toString(), step, getRef());
//				machine.tell(req, getRef());
//				expectMsgAnyClassOf(Duration.ofSeconds(3), ReadyForProcessEvent.class);
//				machine.tell(new LockForOrder(step.toString(), "Order1"), getRef());
//				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); // producting
//				expectMsgAnyClassOf(Duration.ofSeconds(7), MachineUpdateEvent.class); // completing
//				expectMsgAnyClassOf(Duration.ofSeconds(2), MachineUpdateEvent.class); // idle
			}	
		};
	}
	

	
	public static AbstractCapability composeInOne(AbstractCapability ...caps) {
		ComparableCapability ac = new ComparableCapability();		
		for (AbstractCapability cap : caps) {
			ac.getCapabilities().add(cap);
		}
		return ac;
	}
	
	public static Actor getDefaultMachineActor(int id) {
		Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
		actor.setID("MockMachineActor"+id);
		actor.setActorName("MockMachineActor"+id);
		actor.setDisplayName("MockMachineActor"+id);
		actor.setUri("http://fiab.actors/MockMachineActor"+id);
		return actor;
	}
	

	
}
