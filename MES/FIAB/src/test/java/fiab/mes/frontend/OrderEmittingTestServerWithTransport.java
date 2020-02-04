package fiab.mes.frontend;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import akka.http.javadsl.HttpsConnectionContext;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.testkit.javadsl.TestKit;
import fiab.mes.DefaultShopfloorInfrastructure;
import fiab.mes.auth.HttpsConfigurator;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.ComparableCapability;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.MachineEntryActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockMachineActor;
import fiab.mes.mockactors.MockMachineWrapper;
import fiab.mes.mockactors.MockPlottingMachineWrapperDelegate;
import fiab.mes.mockactors.TestBasicMachineActor;
import fiab.mes.mockactors.TestMockMachineActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.restendpoint.ActorRestEndpoint;
import fiab.mes.shopfloor.DefaultLayout;

public class OrderEmittingTestServerWithTransport {
	
	private static ActorSystem system;
	private static String ROOT_SYSTEM = "routes";
	private static ActorSelection machineEventBus;
	private static ActorSelection orderEventBus;
	private static ActorSelection orderPlanningActor;
	private static ActorRef orderEntryActor;
	private static ActorRef machineEntryActor;
	private static CompletionStage<ServerBinding> binding;

	private static final Logger logger = LoggerFactory.getLogger(OrderEmittingTestServerWithTransport.class);
	static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
//	private static OrderProcess process;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create(ROOT_SYSTEM);
		final Http http = Http.get(system);
		
		HttpsConnectionContext https = HttpsConfigurator.useHttps(system);
	    http.setDefaultServerHttpContext(https);
		
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
	
	    System.out.println("Server online at https://localhost:8080/");
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
	void testFrontendResponsesByEmittingOrdersSequentialProcess() throws ExecutionException, InterruptedException, IOException {
			new TestKit(system) { 
				{ 
					System.out.println("test frontend responses by emitting orders with sequential process");
					
					orderEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "")), getRef() );
					machineEventBus.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("OrderMock", "*")), getRef() );
			
					new DefaultLayout(system).setupTwoTurntableWith2MachinesAndIO();
					int countConnEvents = 0;
					boolean isPlannerFunctional = false;
					while (!isPlannerFunctional || countConnEvents < 8 ) {
						TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(15), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, PlanerStatusMessage.class); 
						logEvent(te);
						if (te instanceof PlanerStatusMessage && ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
							 isPlannerFunctional = true;
						}
						if (te instanceof MachineConnectedEvent) {
							countConnEvents++; 
							knownActors.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
						}
						if (te instanceof MachineStatusUpdateEvent) {
							if (((MachineStatusUpdateEvent) te).getStatus().equals(MachineStatus.STOPPED)) 
								Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId() ) ).ifPresent(
										actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef())
								);	
						}
					} 
			
				    CountDownLatch count = new CountDownLatch(3);
				    while(count.getCount() > 0) {
				    	String oid = "P"+String.valueOf(count.getCount()+"-");
				    	OrderProcess op1 = new OrderProcess(TestMockMachineActor.getSingleGreenStepProcess(oid));				
						RegisterProcessRequest req = new RegisterProcessRequest(oid, oid, op1, getRef());
				    	orderEntryActor.tell(req, getRef());
				    	
				    	count.countDown();
				    	Thread.sleep(3000);
				    }
				    System.out.println("Finished with emitting orders. Press ENTER to end test!");
				    System.in.read();
				    System.out.println("Test completed");
				}	
			};

	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}

}