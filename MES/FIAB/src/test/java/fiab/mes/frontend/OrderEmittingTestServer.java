package fiab.mes.frontend;

import java.io.IOException;
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
//	private static ActorSelection machineEventBus;
//	private static ActorSelection orderEventBus;
//	private static ActorSelection orderPlanningActor;
	private static ActorRef orderEntryActor;
	private static CompletionStage<ServerBinding> binding;

	private static OrderProcess process;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create(ROOT_SYSTEM);
		final Http http = Http.get(system);
	    final ActorMaterializer materializer = ActorMaterializer.create(system);
	    DefaultShopfloorInfrastructure shopfloor = new DefaultShopfloorInfrastructure(system);
//	    orderEventBus = system.actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
//	    machineEventBus = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
//	    orderPlanningActor = system.actorSelection("/user/"+OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
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
					
					ActorRef red1 = getMachineMockActor(1, "Red");
					ActorRef blue2 = getMachineMockActor(2, "Blue");
					ActorRef green3 = getMachineMockActor(3, "Green");
					ActorRef yellow4 = getMachineMockActor(4, "Yellow");
					
					process = new OrderProcess(TestMockMachineActor.getSequentialProcess());
					
				    CountDownLatch semaphore = new CountDownLatch(10);
				    while(semaphore.getCount() > 0) {
				    	String id = String.valueOf(semaphore.getCount());
				    	System.out.println("Inserting Order"+id);
				    	
				    	RegisterProcessRequest req = new RegisterProcessRequest(id, id, process, getRef());
				    	orderEntryActor.tell(req, getRef());
				    	
				    	semaphore.countDown();
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
		ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		Actor modelActor = TestMockMachineActor.getDefaultMachineActor(id);
		AbstractCapability cap = TestMockMachineActor.composeInOne(TestMockMachineActor.getPlottingCapability(), TestMockMachineActor.getColorCapability(color));
		return system.actorOf(MockMachineActor.props(eventBusByRef, cap, modelActor));
	}

}
