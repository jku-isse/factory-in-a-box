package fiab.mes.frontend;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.testkit.javadsl.TestKit;
import fiab.mes.DefaultShopfloorInfrastructure;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockMachineActor;
import fiab.mes.mockactors.TestMockMachineActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.restendpoint.ActorRestEndpoint;

public class OrderEmittingTestServer {
	
	private static ActorSystem system;
	private static String ROOT_SYSTEM = "routes";
	private static ActorSelection machineEventBus;
	private static ActorSelection orderEventBus;
	private static ActorSelection orderPlanningActor;
	private static ActorRef orderEntryActor;
	private static CompletionStage<ServerBinding> binding;

//	private static OrderProcess process;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create(ROOT_SYSTEM);
		final Http http = Http.get(system);
	    final ActorMaterializer materializer = ActorMaterializer.create(system);
	    DefaultShopfloorInfrastructure shopfloor = new DefaultShopfloorInfrastructure(system);
	    orderEventBus = system.actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	    machineEventBus = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	    orderPlanningActor = system.actorSelection("/user/"+OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
	    orderEntryActor = system.actorOf(OrderEntryActor.props());
	    ActorRestEndpoint app = new ActorRestEndpoint(system, orderEntryActor);
	
	    final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
	    binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8080), materializer);
	
	    System.out.println("Server online at http://localhost:8080/");
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterClass
	public static void teardown() {
		binding
	        .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
	        .thenAccept(unbound -> {
	        	TestKit.shutdownActorSystem(system);
	        }); // and shutdown when done
	    system.terminate();
	    system = null;
	}
	
	@Test
	void testFrontendResponsesByEmittingOrders() {
		try {
			new TestKit(system) { 
				{ 
					System.out.println("testFrontendResponsesByEmittingOrders()");
					
					orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "")), getRef() );
					machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "*")), getRef() );
					//machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", mid)), getRef() );
					// if we subscribe to machinebus then we need to capture all the machine events by those actors
					ActorRef red1 = getMachineMockActor(1, "Red");
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineConnectedEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineUpdateEvent.class); //idle
					ActorRef blue2 = getMachineMockActor(2, "Blue");
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
					ActorRef green3 = getMachineMockActor(3, "Green");
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
					ActorRef yellow4 = getMachineMockActor(4, "Yellow");
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
					expectMsgClass(Duration.ofSeconds(1), MachineUpdateEvent.class); //idle
					
					OrderProcess process = new OrderProcess(TestMockMachineActor.getSequentialProcess());
			    	String processId = "process";
					process.getProcess().setID(processId);	
					
				    CountDownLatch count = new CountDownLatch(1);
				    while(count.getCount() > 0) {
//				    	OrderProcess process = new OrderProcess(TestMockMachineActor.getSequentialProcess());
//				    	String processId = "process"+String.valueOf(count.getCount());
//						process.getProcess().setID(processId);			
				    	RegisterProcessRequest req = new RegisterProcessRequest("", processId, process, getRef());
				    	orderEntryActor.tell(req, getRef());
				    	
				    	count.countDown();
				    	Thread.sleep(3000);
				    }
				    System.out.println("Finished with emitting orders. Press ENTER to end test!");
				    System.in.read();
				    System.out.println("Test completed");
				}	
			};
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private static ActorRef getMachineMockActor(int id, String color) {
		Actor modelActor = TestMockMachineActor.getDefaultMachineActor(id);
		AbstractCapability cap = TestMockMachineActor.composeInOne(TestMockMachineActor.getPlottingCapability(), TestMockMachineActor.getColorCapability(color));
		return system.actorOf(MockMachineActor.props(machineEventBus, cap, modelActor));
	}

}
