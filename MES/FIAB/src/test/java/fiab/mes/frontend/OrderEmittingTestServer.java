package fiab.mes.frontend;

import static akka.pattern.PatternsCS.ask;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import akka.util.Timeout;
import fiab.mes.DefaultShopfloorInfrastructure;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.restendpoint.ActorRestEndpoint;
import scala.concurrent.duration.FiniteDuration;

public class OrderEmittingTestServer {
	
	private static ActorSystem system;
	private static String ROOT_SYSTEM = "routes";
	private static ActorSelection machineEventBus;
	private static ActorSelection orderEventBus;
	private static ActorSelection orderPlanningActor;
	private static ActorRef orderEntryActor;
	private static CompletionStage<ServerBinding> binding;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		// setup shopfloor
		// setup machines
		// setup processes
		// setup order actors?
		// add processes to orderplanning actor
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
	
	@SuppressWarnings("deprecation")
	@Test
	void testFrontendResponsesByEmittingOrders() {
		try {
			new TestKit(system) { 
				{ 
					System.out.println("testFrontendResponsesByEmittingOrders()");
				    CountDownLatch semaphore = new CountDownLatch(10);
				    final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));
				    while(semaphore.getCount() > 0) {
				    	System.out.println("create new order:");
				    	ask(orderEntryActor, getRegisterProcessRequest(String.valueOf(semaphore.getCount()), system), timeout);
				    	semaphore.countDown();
				    	Thread.sleep(3000);
				    }
				    System.in.read();
				    System.out.println("Test completed");
				}	
			};
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private RegisterProcessRequest getRegisterProcessRequest(String id, ActorSystem system) {
		setupSteps();
	}

}
