package fiab.mes.restendpoint;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import fiab.mes.DefaultShopfloorInfrastructure;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderActor;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import scala.concurrent.duration.FiniteDuration;

import static akka.pattern.PatternsCS.ask;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ParallelBranches;
import ProcessCore.ProcessCoreFactory;

public class MinimalTestServer extends AllDirectives {

  
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
	    // boot up server using the route as defined below
	    ActorSystem system = ActorSystem.create("routes");
	      
	    final Http http = Http.get(system);
	    final ActorMaterializer materializer = ActorMaterializer.create(system);
	
	    DefaultShopfloorInfrastructure shopfloor = new DefaultShopfloorInfrastructure(system);
	    ActorRef orderEntryActor = system.actorOf(OrderEntryActor.props());
	    ActorRestEndpoint app = new ActorRestEndpoint(system, orderEntryActor);
	
	    final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
	    final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8080), materializer);
	
	    System.out.println("Server online at http://localhost:8080/\nstart emitting orders...");
	    CountDownLatch semaphore = new CountDownLatch(10);
	    final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));
	    while(semaphore.getCount() > 0) {
	    	System.out.println("create new order:");
	    	ask(orderEntryActor, getOrder(String.valueOf(semaphore.getCount()), system), timeout);
	    	semaphore.countDown();
	    	Thread.sleep(3000);
	    }
	    System.in.read();
	    binding
	        .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
	        .thenAccept(unbound -> system.terminate()); // and shutdown when done
	}

	private static RegisterProcessRequest getOrder(String id, ActorSystem system) {
		setupSteps();
		OrderProcess process = new OrderProcess(getSequentialProcess());
		RegisterProcessRequest processRequest = new RegisterProcessRequest(id , id, process, null);
//		ActorSelection eventBusByRef = system.actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
//		ActorSelection orderPlannerByRef = system.actorSelection("/user/"+OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
//		ActorRef orderActor = system.actorOf(OrderActor.props(processRequest, eventBusByRef, orderPlannerByRef));
//		processRequest.setRequestor(orderActor);
		return processRequest;
	}
	
	public static CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	public static CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	public static CapabilityInvocation s3 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	public static CapabilityInvocation s4 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
	
	private static ProcessCore.Process getSequentialProcess() {
		ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
		p.getSteps().add(s1);
		p.getSteps().add(s2);
		p.getSteps().add(s3);
		p.getSteps().add(s4);
		return p;
	}
	
	private static ProcessCore.Process getParallelProcess() {
		ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
		ParallelBranches paraB = ProcessCoreFactory.eINSTANCE.createParallelBranches();
		p.getSteps().add(paraB);
		
		ProcessCore.Process paraP1 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP1.getSteps().add(s1);
		paraB.getBranches().add(paraP1);
		ProcessCore.Process paraP2 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP2.getSteps().add(s2);
		paraB.getBranches().add(paraP2);
		ProcessCore.Process paraP3 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP3.getSteps().add(s3);
		paraB.getBranches().add(paraP3);
		ProcessCore.Process paraP4 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP4.getSteps().add(s4);
		paraB.getBranches().add(paraP4);
		return p;
	}

	public static void setupSteps() {
		s1.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Red")));
		s2.setInvokedCapability(composeInOne(getPlottingCapability(),getColorCapability("Blue")));		
		s3.setInvokedCapability(composeInOne(getPlottingCapability(),getColorCapability("Green")));		
		s4.setInvokedCapability(composeInOne(getPlottingCapability(),getColorCapability("Yellow")));
	}
	

	private static AbstractCapability getColorCapability(String color) {
		AbstractCapability ac = ProcessCoreFactory.eINSTANCE.createAbstractCapability();
		ac.setDisplayName(color);
		ac.setID("Capability.Plotting.Color."+color);
		ac.setID("http://factory-in-a-box.fiab/capabilities/plotter/colors/"+color);
		return ac;
	}
	
	private static AbstractCapability getPlottingCapability() {
		AbstractCapability ac = ProcessCoreFactory.eINSTANCE.createAbstractCapability();
		ac.setDisplayName("plot");
		ac.setID("Capability.Plotting");
		ac.setID("http://factory-in-a-box.fiab/capabilities/plotter/plotting");
		return ac;
	}
	
	private static AbstractCapability composeInOne(AbstractCapability ...caps) {
		AbstractCapability ac = ProcessCoreFactory.eINSTANCE.createAbstractCapability();		
		for (AbstractCapability cap : caps) {
			ac.getCapabilities().add(cap);
		}
		return ac;
	}
}