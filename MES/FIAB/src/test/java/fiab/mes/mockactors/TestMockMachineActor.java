package fiab.mes.mockactors;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.ProcessStep;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.ComparableCapability;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;

public class TestMockMachineActor { //extends AbstractJavaTest {

	protected static ActorSystem system;
	protected static ActorRef machine;
	public static String ROOT_SYSTEM = "routes";
	protected OrderProcess op;
	
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
	void testRegisterProcess() {
		final AbstractCapability cap = composeInOne(getPlottingCapability(), getColorCapability("Red"));
		final Actor modelActor = getDefaultMachineActor(1);
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				//eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				machine = system.actorOf(MockMachineActor.props(eventBusByRef, cap, modelActor));
				//expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
				ProcessStep step = op.getAvailableSteps().get(0);
				RegisterProcessStepRequest req = new RegisterProcessStepRequest("Order1", step.toString(), step, getRef());
				machine.tell(req, getRef());
				expectMsgAnyClassOf(Duration.ofSeconds(3), ReadyForProcessEvent.class);
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
	
	public static CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	public static CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	public static CapabilityInvocation s3 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	public static CapabilityInvocation s4 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();

	public static ProcessCore.Process getSequentialProcess() {
		s1.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Red")));
		s2.setInvokedCapability(composeInOne(getPlottingCapability(),getColorCapability("Blue")));		
		s3.setInvokedCapability(composeInOne(getPlottingCapability(),getColorCapability("Green")));		
		s4.setInvokedCapability(composeInOne(getPlottingCapability(),getColorCapability("Yellow")));		
		
		ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
		p.getSteps().add(s1);
		p.getSteps().add(s2);
		p.getSteps().add(s3);
		p.getSteps().add(s4);

		return p;
	}
	
}
