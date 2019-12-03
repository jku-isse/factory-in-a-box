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
import ProcessCore.CapabilityInvocation;
import ProcessCore.ParallelBranches;
import ProcessCore.ProcessCoreFactory;
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
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.ComparableCapability;
import fiab.mes.machine.MachineEntryActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockMachineActor;
import fiab.mes.mockactors.MockMachineWrapper;
import fiab.mes.mockactors.MockPlottingMachineWrapperDelegate;
import fiab.mes.mockactors.TestBasicMachineActor;
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
	private static ActorRef machineEntryActor;
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
	    machineEntryActor = system.actorOf(MachineEntryActor.props());
	    ActorRestEndpoint app = new ActorRestEndpoint(system, orderEntryActor, machineEntryActor);
	
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
	void testFrontendResponsesByEmittingOrdersSequentialProcess() {
		try {
			new TestKit(system) { 
				{ 
					System.out.println("test frontend responses by emitting orders with sequential process");
					
					orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "")), getRef() );
					machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "*")), getRef() );

					ActorRef red1 = getMachineMockActor(1, "Red");
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineConnectedEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);
					ActorRef blue2 = getMachineMockActor(2, "Blue");
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineStatusUpdateEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);
					ActorRef green3 = getMachineMockActor(3, "Green");
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineStatusUpdateEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);
					ActorRef yellow4 = getMachineMockActor(4, "Yellow");
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
					expectMsgClass(Duration.ofSeconds(1), MachineStatusUpdateEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);

				    CountDownLatch count = new CountDownLatch(5);
				    while(count.getCount() > 0) {
				    	OrderProcess process = new OrderProcess(TestBasicMachineActor.getSequentialProcess());
				    	String processId = "process"+String.valueOf(count.getCount());
						process.getProcess().setID(processId);			
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
	
	@Test
	void testFrontendResponsesByEmittingOrdersParallelProcess() {
		try {
			new TestKit(system) { 
				{ 
					System.out.println("test frontend responses by emitting orders with parallel process");
					
					orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "")), getRef() );
					machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "*")), getRef() );

					ActorRef red1 = getMachineMockActor(1, "Red");
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineConnectedEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);
					ActorRef blue2 = getMachineMockActor(2, "Blue");
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineStatusUpdateEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);
					ActorRef green3 = getMachineMockActor(3, "Green");
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineStatusUpdateEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);
					ActorRef yellow4 = getMachineMockActor(4, "Yellow");
					expectMsgAnyClassOf(Duration.ofSeconds(1), MachineConnectedEvent.class);
					expectMsgClass(Duration.ofSeconds(1), MachineStatusUpdateEvent.class);
					expectMsgAnyClassOf(Duration.ofSeconds(3), MachineStatusUpdateEvent.class);

				    CountDownLatch count = new CountDownLatch(5);
				    while(count.getCount() > 0) {
				    	OrderProcess process = new OrderProcess(getParallelProcess("1-"));
				    	String processId = "process"+String.valueOf(count.getCount());
						process.getProcess().setID(processId);			
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
		AbstractCapability cap = TestBasicMachineActor.composeInOne(TestBasicMachineActor.getPlottingCapability(), TestBasicMachineActor.getColorCapability(color));		
		Actor modelActor = TestBasicMachineActor.getDefaultMachineActor(id);
		InterMachineEventBus intraEventBus = new InterMachineEventBus();
		ActorRef machineWrapper = system.actorOf(MockMachineWrapper.props(intraEventBus));
		PlottingMachineWrapperInterface wrapperDelegate = new MockPlottingMachineWrapperDelegate(machineWrapper);
		return system.actorOf(BasicMachineActor.props(machineEventBus, cap, modelActor, wrapperDelegate, intraEventBus));
	}
	
	private ProcessCore.Process getParallelProcess(String prefix) {
		CapabilityInvocation s1 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		CapabilityInvocation s2 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		CapabilityInvocation s3 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		CapabilityInvocation s4 = ProcessCoreFactory.eINSTANCE.createCapabilityInvocation();
		s1.setID(prefix+"1");
		s2.setID(prefix+"2");
		s3.setID(prefix+"3");
		s4.setID(prefix+"4");
		s1.setDisplayName("Red");
		s2.setDisplayName("Blue plotting");
		s3.setDisplayName("Green plotting");
		s4.setDisplayName("Yellow plotting");
		s1.setInvokedCapability(getColorCapability("Red"));
		s2.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Blue")));		
		s3.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Green")));		
		s4.setInvokedCapability(composeInOne(getPlottingCapability(), getColorCapability("Yellow")));
		
		ProcessCore.Process p = ProcessCoreFactory.eINSTANCE.createProcess();
		ParallelBranches paraB = ProcessCoreFactory.eINSTANCE.createParallelBranches();
		p.getSteps().add(paraB);
		
		ProcessCore.Process paraP1 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP1.setID(prefix+"1");
		paraP1.setDisplayName("Process 1");
		paraP1.getSteps().add(s1);
		paraB.getBranches().add(paraP1);
		ProcessCore.Process paraP2 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP2.setID(prefix+"2");
		paraP2.setDisplayName("Process 2");
		paraP2.getSteps().add(s2);
		paraB.getBranches().add(paraP2);
		ProcessCore.Process paraP3 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP3.setID(prefix+"3");
		paraP3.setDisplayName("Process 3");
		paraP3.getSteps().add(s3);
		paraB.getBranches().add(paraP3);
		ProcessCore.Process paraP4 = ProcessCoreFactory.eINSTANCE.createProcess();
		paraP4.setID(prefix+"4");
		paraP4.setDisplayName("Process 4");
		paraP4.getSteps().add(s4);
		paraB.getBranches().add(paraP4);
		return p;
	}
	
	private AbstractCapability getColorCapability(String color) {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName(color);
		ac.setID("Capability.Plotting.Color."+color);
		ac.setID("http://factory-in-a-box.fiab/capabilities/plotter/colors/"+color);
		return ac;
	}
	
	private AbstractCapability getPlottingCapability() {
		ComparableCapability ac = new ComparableCapability();
		ac.setDisplayName("Plot");
		ac.setID("Capability.Plotting");
		ac.setID("http://factory-in-a-box.fiab/capabilities/plotter/plotting");
		return ac;
	}
	
	private AbstractCapability composeInOne(AbstractCapability ...caps) {
		ComparableCapability ac = new ComparableCapability();		
		for (AbstractCapability cap : caps) {
			ac.getCapabilities().add(cap);
		}
		return ac;
	}

}
