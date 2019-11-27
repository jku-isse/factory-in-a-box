package fiab.mes.mockactors;

import java.time.Duration;

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
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.ComparableCapability;
import fiab.mes.general.TimedEvent;
import fiab.mes.handshake.HandshakeProtocol.ServerSide;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockServerHandshakeActor.MessageTypes;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;

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
		ProcessCore.Process p = getSequentialProcess();
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
		final AbstractCapability cap = composeInOne(getPlottingCapability(), getColorCapability("Red"));
		final Actor modelActor = getDefaultMachineActor(1);
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				//eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				
				InterMachineEventBus intraEventBus = new InterMachineEventBus();
				ActorRef machineWrapper = system.actorOf(MockTransportAwareMachineWrapper.props(intraEventBus), "MachineWrapper1");
				ActorSelection serverSide = system.actorSelection("/user/MachineWrapper1/ServerSideHandshakeMock");
				PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
				machine = system.actorOf(BasicMachineActor.props(eventBusByRef, cap, modelActor, wrapperDelegate, intraEventBus));
				// we subscribe to the intraeventbus to observe wrapper behavior
				intraEventBus.subscribe(getRef(), new SubscriptionClassifier("TestClass", "*"));
				
				boolean doRun = true;
				while (doRun) {
					MachineStatusUpdateEvent mue = expectMsgClass(Duration.ofSeconds(3600), MachineStatusUpdateEvent.class);
					logEvent(mue);
					MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
					if (newState.equals(MachineStatus.IDLE)) {
						machine.tell(new LockForOrder("TestStep1","TestRootOrder1"), getRef()); // here we dont register and wait for readyness, wont work later
					}
					if (newState.equals(MachineStatus.STARTING)) {
						boolean handshakeDone = false;
						serverSide.tell(MockServerHandshakeActor.MessageTypes.SubscribeToStateUpdates, getRef());
						while (!handshakeDone) {
							ServerSide state = expectMsgClass(Duration.ofSeconds(5), ServerSide.class);
							switch(state) {
							case IdleEmpty:
								serverSide.tell(MockServerHandshakeActor.MessageTypes.RequestInitiateHandover, getRef());
								expectMsg(Duration.ofSeconds(5), ServerSide.Starting);
								expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseInitHandover);
								break;
							case ReadyEmpty:
								serverSide.tell(MessageTypes.RequestStartHandover, getRef());
								expectMsg(Duration.ofSeconds(5), ServerSide.Execute);
								expectMsg(Duration.ofSeconds(5), MessageTypes.OkResponseStartHandover);
								serverSide.tell(MockServerHandshakeActor.MessageTypes.UnsubscribeToStateUpdates, getRef()); //otherwise the handshake events interfere with other expected events
								handshakeDone = true; // part until where we need to be involved, thanks to autocomplete
								break;
							default:
								break;
							}
						}
					}
					if (newState.equals(MachineStatus.COMPLETING)) {
						doRun = false;
					}
				}
				//expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
			}	
		};
	}
		
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}
	
	@Test
	void testRegisterProcess() {
		final AbstractCapability cap = composeInOne(getPlottingCapability(), getColorCapability("Red"));
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
				boolean isIdle = false;
				while (!isIdle) {
					TimedEvent te = expectMsgClass(Duration.ofSeconds(3600), TimedEvent.class);
					logEvent(te);
				}
				//expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
				ProcessStep step = op.getAvailableSteps().get(0);
				RegisterProcessStepRequest req = new RegisterProcessStepRequest("Order1", step.toString(), step, getRef());
				machine.tell(req, getRef());
				expectMsgAnyClassOf(Duration.ofSeconds(5), ReadyForProcessEvent.class);
			}	
		};
	}
		
	
	@Test
	void testLockForOrder() {
		final AbstractCapability cap = composeInOne(getPlottingCapability(), getColorCapability("Red"));
		final Actor modelActor = getDefaultMachineActor(1);
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				machine = system.actorOf(MockMachineActor.props(eventBusByRef, cap, modelActor));
				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
				ProcessStep step = op.getAvailableSteps().get(0);
				RegisterProcessStepRequest req = new RegisterProcessStepRequest("Order1", step.toString(), step, getRef());
				machine.tell(req, getRef());
				expectMsgAnyClassOf(Duration.ofSeconds(3), ReadyForProcessEvent.class);
				machine.tell(new LockForOrder(step.toString(), "Order1"), getRef());
				expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); // producting
				expectMsgAnyClassOf(Duration.ofSeconds(7), MachineUpdateEvent.class); // completing
				expectMsgAnyClassOf(Duration.ofSeconds(2), MachineUpdateEvent.class); // idle
			}	
		};
	}
	
	// ensure to keep this in sync with OrderProcessTest
	public static AbstractCapability getColorCapability(String color) {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName(color);
		ac.setID("Capability.Plotting.Color."+color);
		ac.setID("http://factory-in-a-box.fiab/capabilities/plotter/colors/"+color);
		return ac;
	}
	
	public static AbstractCapability getPlottingCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("plot");
		ac.setID("Capability.Plotting");
		ac.setID("http://factory-in-a-box.fiab/capabilities/plotter/plotting");
		return ac;
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
	
//	public static CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
//	public static CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
//	public static CapabilityInvocation s3 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
//	public static CapabilityInvocation s4 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();

	public static ProcessCore.Process getSequentialProcess() {
		return getSequentialProcess("1-");
	}
	
	public static ProcessCore.Process getSequentialProcess(String prefix) {
		CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		CapabilityInvocation s3 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		CapabilityInvocation s4 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		s1.setID(prefix+"1");
		s2.setID(prefix+"2");
		s3.setID(prefix+"3");
		s4.setID(prefix+"4");
		s1.setDisplayName("red plotting");
		s2.setDisplayName("blue plotting");
		s3.setDisplayName("green plotting");
		s4.setDisplayName("yellow plotting");
		s1.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Red")));
		s2.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Blue")));		
		s3.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Green")));		
		s4.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Yellow")));		
		
		ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
		p.getSteps().add(s1);
		p.getSteps().add(s2);
		p.getSteps().add(s3);
		p.getSteps().add(s4);

		return p;
	}
	
}
