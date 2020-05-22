package fiab.mes.eventbus;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ProcessCore.ProcessCoreFactory;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.OrderProcess.StepStatusEnum;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.OrderProcessUpdateEvent;

public class TestOrderEventBus { //extends AbstractJavaTest {

	protected static ActorSystem system;
	protected static ActorRef eventBus;
	public static String ROOT_SYSTEM = "routes";
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		
		system = ActorSystem.create(ROOT_SYSTEM);
		eventBus = system.actorOf(OrderEventBusWrapperActor.props(), OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		System.out.println(eventBus.path().parent().toStringWithoutAddress());
	}

	@BeforeEach
	void setUp() throws Exception {
		
	}
	
	@AfterClass
	public static void teardown() {
	    TestKit.shutdownActorSystem(system);
	    system = null;
	}

	@Test
	void testPublishOrderEvent() {
		OrderEvent oe = new OrderEvent("TestOrder1", "TestMachine1", OrderEventType.CREATED, "");
		eventBus.tell(oe, ActorRef.noSender());
		
		ProcessChangeImpact pci = new ProcessChangeImpact(ProcessCoreFactory.eINSTANCE.createCapabilityInvocation(), StepStatusEnum.INITIATED, StepStatusEnum.AVAILABLE);		
		OrderProcessUpdateEvent opue = new OrderProcessUpdateEvent("TestOrder2", "TestMachine1", "", pci);
		system.actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME).tell(opue, ActorRef.noSender());		
	}
	
	@Test
	void testSubscribeAndReceiveOrderEvent() {
		new TestKit(system) { 
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);		    	
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("TestMachine2", "*")), getRef() );
				OrderEvent oe = new OrderEvent("TestOrder1", "TestMachine1", OrderEventType.CREATED, "");
				eventBusByRef.tell(oe, getRef());				
				expectMsg(Duration.ofSeconds(3), oe);				
		    }
		};
	}

}
