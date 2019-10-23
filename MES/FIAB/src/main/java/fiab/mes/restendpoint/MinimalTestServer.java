package fiab.mes.restendpoint;

import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.AbstractActor.Receive;
import akka.event.Logging;
import akka.event.LoggingAdapter;
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
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcess.ProcessChangeImpact;
import fiab.mes.order.actor.OrderActor;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;
import fiab.mes.planer.actor.OrderPlanningActor;
import scala.concurrent.duration.FiniteDuration;

import static akka.pattern.PatternsCS.ask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.ParallelBranches;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.ProcessStep;

public class MinimalTestServer extends AllDirectives {

	private static ActorSelection orderEventBusByRef;
	private static ActorSelection machineEventBusByRef;
	private static ActorSelection orderPlannerByRef;
	private static ActorSystem system;
	private static OrderProcess process;
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
	    // boot up server using the route as defined below
	    system = ActorSystem.create("routes");
	      
	    final Http http = Http.get(system);
	    final ActorMaterializer materializer = ActorMaterializer.create(system);
	
	    DefaultShopfloorInfrastructure shopfloor = new DefaultShopfloorInfrastructure(system);
	    orderEventBusByRef = system.actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	    machineEventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	    orderPlannerByRef = system.actorSelection("/user/"+OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
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
	    	Thread.sleep(5000);
	    	process.activateProcess();
	    	ProcessChangeImpact pci = process.markStepComplete(s1);
			OrderProcessUpdateEvent opue = new OrderProcessUpdateEvent("Order1", "TestMachine1", pci);
			system.actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME).tell(opue, ActorRef.noSender());
			
			Thread.sleep(3000);
	    }
	    System.in.read();
	    binding
	        .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
	        .thenAccept(unbound -> system.terminate()); // and shutdown when done
	}

	private static RegisterProcessRequest getOrder(String id, ActorSystem system) {
		setupSteps();
		ActorRef red1 = getMachineMockActor(1, "Red", s1);
		ActorRef blue2 = getMachineMockActor(2, "Blue", s2);
		ActorRef green3 = getMachineMockActor(3, "Green", s3);
		ActorRef yellow4 = getMachineMockActor(4, "Yellow", s4);
		
		machineEventBusByRef.tell(new SubscribeMessage(red1, new SubscriptionClassifier("OrderMock", "*")), red1 );
		machineEventBusByRef.tell(new SubscribeMessage(blue2, new SubscriptionClassifier("OrderMock", "*")), blue2 );
		machineEventBusByRef.tell(new SubscribeMessage(green3, new SubscriptionClassifier("OrderMock", "*")), green3 );
		machineEventBusByRef.tell(new SubscribeMessage(yellow4, new SubscriptionClassifier("OrderMock", "*")), yellow4 );
		process = new OrderProcess(getSequentialProcess());
		
		RegisterProcessRequest processRequest = new RegisterProcessRequest(id , id, process, null);
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
		s2.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Blue")));		
		s3.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Green")));		
		s4.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Yellow")));
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
	
	private static ActorRef getMachineMockActor(int id, String color, CapabilityInvocation cap) {
		Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
		actor.setID("MockMachineActor"+id);
		actor.setActorName("MockMachineActor"+id);
		actor.setDisplayName("MockMachineActor"+id);
		actor.setUri("http://fiab.actors/MockMachineActor"+id);
		Actor modelActor = actor;
		return system.actorOf(MockMachineActor.props(orderEventBusByRef, cap.getInvokedCapability(), modelActor));
	}
	
	public static class MockMachineActor extends AbstractActor{

		private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
		protected ActorSelection eventBusByRef;
		protected final AkkaActorBackedCoreModelAbstractActor machineId;
		protected AbstractCapability cap;
		protected String currentState;
		
		protected List<RegisterProcessStepRequest> orders = new ArrayList<>();
		
		public static Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor) {	    
			return Props.create(MockMachineActor.class, () -> new MockMachineActor(machineEventBus, cap, modelActor));
		}
		
		public MockMachineActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor) {
			this.cap = cap;
			this.machineId = new AkkaActorBackedCoreModelAbstractActor(modelActor.getID(), modelActor, self());
			this.eventBusByRef = machineEventBus;
			init();
			setAndPublishNewState(MachineOrderMappingManager.IDLE_STATE_VALUE);
		}
		
		@Override
		public Receive createReceive() {
			return receiveBuilder()
			        .match(RegisterProcessStepRequest.class, registerReq -> {
			        	orders.add(registerReq);
			        	log.info(String.format("Job %s of Order %s registered.", registerReq.getProcessStepId(), registerReq.getRootOrderId()));
			        	checkIfAvailableForNextOrder();
			        } )
			        .match(LockForOrder.class, lockReq -> {
			        	if (currentState == MachineOrderMappingManager.IDLE_STATE_VALUE) {
			        		//TODO: here we assume correct invocation: thus on overtaking etc, will be improved later
			        		setAndPublishNewState(MachineOrderMappingManager.PRODUCING_STATE_VALUE); // we skip starting state here  
			        		finishProduction();
			        	} else {
			        		log.warning("Received lock for order in state: "+currentState);
			        	}
			        })
			        .build();
		}

		private void init() {
			eventBusByRef.tell(new MachineConnectedEvent(machineId, Collections.singleton(cap), Collections.emptySet()), self());
		}
		
		private void setAndPublishNewState(String newState) {
			this.currentState = newState;
			eventBusByRef.tell(new MachineUpdateEvent(machineId.getId(), null, MachineOrderMappingManager.STATE_VAR_NAME, newState), self());
		}
		
		private void checkIfAvailableForNextOrder() {
			if (currentState == MachineOrderMappingManager.IDLE_STATE_VALUE && !orders.isEmpty()) { // if we are idle, tell next order to get ready, this logic is also triggered upon machine signaling completion
				RegisterProcessStepRequest ror = orders.remove(0);
				log.info("Ready for next Order: "+ror.getRootOrderId());
	    		ror.getRequestor().tell(new ReadyForProcessEvent(ror), getSelf());
	    	}	
		}	
		
		private void finishProduction() {
			context().system()
	    	.scheduler()
	    	.scheduleOnce(Duration.ofMillis(5000), 
	    			 new Runnable() {
	            @Override
	            public void run() {
	            	setAndPublishNewState(MachineOrderMappingManager.COMPLETING_STATE_VALUE); 
	            	resetToIdle();
	            }
	          }, context().system().dispatcher());
		}
		
		private void resetToIdle() {
			context().system()
	    	.scheduler()
	    	.scheduleOnce(Duration.ofMillis(1000), 
	    			 new Runnable() {
	            @Override
	            public void run() {
	            	setAndPublishNewState(MachineOrderMappingManager.IDLE_STATE_VALUE); // we then skip completed state
	            	checkIfAvailableForNextOrder();
	            }
	          }, context().system().dispatcher());
		}
	}

}