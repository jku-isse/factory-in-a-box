package fiab.mes.frontend;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.http.javadsl.ServerBinding;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.tracing.TestTracingUtil;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.ShopfloorStartup;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.msg.PlanerStatusMessage;
import fiab.mes.planer.msg.PlanerStatusMessage.PlannerState;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.transport.msg.TransportSystemStatusMessage;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;

public class OrderEmittingTestServerWithOPCUA {

	private static ActorSystem system;
	private static String ROOT_SYSTEM = "routes";
	private static ActorSelection machineEventBus;
	private static ActorSelection orderEventBus;
	private static ActorSelection orderEntryActor;
	private static ActorSelection transportSystemCoordinatorActor;
	private static CompletionStage<ServerBinding> binding;

	private static final Logger logger = LoggerFactory.getLogger(OrderEmittingTestServerWithOPCUA.class);
	static HashMap<String, AkkaActorBackedCoreModelAbstractActor> knownActors = new HashMap<>();
//	private static OrderProcess process;

	public static void main(String args[]) {
		// Dual TT tests:
//		TestTurntableWithIOStations.startupW34toE35();

	}

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		system = ActorSystem.create(ROOT_SYSTEM);
//		system.registerExtension(TestTracingUtil.getTracingExtension());

		binding = ShopfloorStartup.startup(null, 1, system);
		orderEventBus = system.actorSelection("/user/" + OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		machineEventBus = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderEntryActor = system.actorSelection("/user/" + OrderEntryActor.WELLKNOWN_LOOKUP_NAME);// .resolveOne(Timeout.create(Duration.ofSeconds(3)))..;
		transportSystemCoordinatorActor = system
				.actorSelection("/user/" + TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterClass
	public static void teardown() {
		binding.thenCompose(ServerBinding::unbind) // trigger unbinding from the port
				.thenAccept(unbound -> {
					TestKit.shutdownActorSystem(system);
				}); // and shutdown when done
		system.terminate();
		system = null;
	}

	@Test // works
	void testFrontendResponsesByEmittingOrdersSequentialProcess() throws Exception {
		new TestKit(system) {
			{
				System.out.println("test frontend responses by emitting orders with sequential process");

				orderEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")),
						getRef());
				machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")),
						getRef());

				Set<String> urlsToBrowse = getTracingLocalhostLayout();
				// Set<String> urlsToBrowse = getSingleTTLayout(); //set layout to 1 expectedTT
				// in preTEst method
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);

				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});

				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				boolean isTransportFunctional = false;
				while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() || !isTransportFunctional) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class);
					logEvent(te);
					if (te instanceof PlanerStatusMessage
							&& ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						isPlannerFunctional = true;
					}
					if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState()
							.equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
						isTransportFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++;
						knownActors.put(((MachineConnectedEvent) te).getMachineId(),
								((MachineConnectedEvent) te).getMachine());
					}
					// DO THIS MANUALLY FROM WEB UI!
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
							Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId()))
									.ifPresent(actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(
											((MachineStatusUpdateEvent) te).getMachineId()), getRef()));
					}
				}

				CountDownLatch count = new CountDownLatch(1);
				while (count.getCount() > 0) {
					String oid = "P" + String.valueOf(count.getCount() + "-");
					OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleBlackStepProcess(oid));
					// OrderProcess op1 = new
					// OrderProcess(ProduceProcess.getSequential4ColorProcess(oid));
					RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, getRef());
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

	@Test // works
	void testForMachineRelocation() throws Exception {
		new TestKit(system) {
			{
				System.out.println("test frontend responses by emitting orders with sequential process");

				orderEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")),
						getRef());
				machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")),
						getRef());

				Set<String> urlsToBrowse = getFullLayout();
				// Set<String> urlsToBrowse = getSingleTTLayout(); //set layout to 1 expectedTT
				// in preTEst method
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);

				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});

				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				boolean isTransportFunctional = false;
				while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() - 1 || !isTransportFunctional) { // we
																														// expect
																														// one
																														// machine
																														// less,
																														// the
																														// spot
																														// we
																														// switch
																														// to,
																														// but
																														// which
																														// we
																														// nevertheless
																														// monitor
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class);
					logEvent(te);
					if (te instanceof PlanerStatusMessage
							&& ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						isPlannerFunctional = true;
					}
					if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState()
							.equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
						isTransportFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++;
						knownActors.put(((MachineConnectedEvent) te).getMachineId(),
								((MachineConnectedEvent) te).getMachine());
					}
					// DO THIS MANUALLY FROM WEB UI!
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
							Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId()))
									.ifPresent(actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(
											((MachineStatusUpdateEvent) te).getMachineId()), getRef()));
					}
				}

				CountDownLatch count = new CountDownLatch(4);
				while (count.getCount() > 0) {
					String oid = "P" + String.valueOf(count.getCount() + "-");
					OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleBlackStepProcess(oid));
					RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, getRef());
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

//	 I/O, TT1 & TT2 
	// keine connections zu den plotters, keine plotters hochfahren
	// TransportSystemCoordinator bereit: wenn beide Turntables dabei sind
	//
	@Test // works
	void testFrontendResponsesByEmittingTransportRequest() throws Exception {
		new TestKit(system) {
			{
				System.out.println("test frontend responses by emitting orders with sequential process");

				orderEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")),
						getRef());
				machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("OrderMock", "*")),
						getRef());

				Set<String> urlsToBrowse = getTracingLocalhostLayout();
				// Set<String> urlsToBrowse = getSingleTTLayout(); //set layout to 1 expectedTT
				// in preTEst method
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);

				urlsToBrowse.stream().forEach(url -> {
					ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
					discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
				});

				int countConnEvents = 0;
				boolean isPlannerFunctional = false;
				boolean isTransportFunctional = false;
				while (!isPlannerFunctional || countConnEvents < urlsToBrowse.size() || !isTransportFunctional) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class);
					logEvent(te);
					if (te instanceof PlanerStatusMessage
							&& ((PlanerStatusMessage) te).getState().equals(PlannerState.FULLY_OPERATIONAL)) {
						isPlannerFunctional = true;
					}
					if (te instanceof TransportSystemStatusMessage && ((TransportSystemStatusMessage) te).getState()
							.equals(TransportSystemStatusMessage.State.FULLY_OPERATIONAL)) {
						isTransportFunctional = true;
					}
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++;
						knownActors.put(((MachineConnectedEvent) te).getMachineId(),
								((MachineConnectedEvent) te).getMachine());
					}
					// DO THIS MANUALLY FROM WEB UI!
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
							Optional.ofNullable(knownActors.get(((MachineStatusUpdateEvent) te).getMachineId()))
									.ifPresent(actor -> actor.getAkkaActor().tell(new GenericMachineRequests.Reset(
											((MachineStatusUpdateEvent) te).getMachineId()), getRef()));
					}
				}

				CountDownLatch count = new CountDownLatch(1);
				while (count.getCount() > 0) {
					String oid = "P" + String.valueOf(count.getCount() + "-");
					OrderProcess op1 = new OrderProcess(ProduceProcess.getSingleBlackStepProcess(oid));
					// OrderProcess op1 = new
					// OrderProcess(ProduceProcess.getSequential4ColorProcess(oid));
					RegisterProcessRequest req = new RegisterProcessRequest(oid, op1, getRef());
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

	public Set<String> getSingleTTLayout() {
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); // Pos34 west inputstation
		urlsToBrowse.add("opc.tcp://192.168.0.31:4840"); // Pos31 TT1 north plotter
		urlsToBrowse.add("opc.tcp://192.168.0.37:4840"); // Pos31 TT1 south plotter
		urlsToBrowse.add("opc.tcp://192.168.0.23:4840"); // POS EAST CLIENT TT1 outputstation instead of second TT
		urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo"); // Pos20 TT
		return urlsToBrowse;
	}

	public Set<String> get3134352021Layout() {
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); // Pos34 west inputstation
		urlsToBrowse.add("opc.tcp://192.168.0.31:4840"); // Pos31 TT1 north plotter
		urlsToBrowse.add("opc.tcp://192.168.0.32:4840"); // Pos32 TT2 north plotter
		urlsToBrowse.add("opc.tcp://192.168.0.35:4840"); // POS EAST 35/ outputstation
		urlsToBrowse.add("opc.tcp://192.168.0.21:4842/milo"); // POS EAST 35/ outputstation
		urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo"); // Pos20 TT
		return urlsToBrowse;
	}

	public Set<String> getFullLayout() {
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); // Pos34 west inputstation
		urlsToBrowse.add("opc.tcp://192.168.0.31:4840"); // Pos31 TT1 north plotter
		urlsToBrowse.add("opc.tcp://192.168.0.32:4840"); // Pos32 TT2 north plotter
		urlsToBrowse.add("opc.tcp://192.168.0.37:4840"); // Pos37 TT1 south plotter
		urlsToBrowse.add("opc.tcp://192.168.0.38:4840"); // Pos38 TT2 south plotter
		urlsToBrowse.add("opc.tcp://192.168.0.35:4840"); // POS EAST 35/ outputstation
		urlsToBrowse.add("opc.tcp://192.168.0.21:4842/milo"); // POS 21 TT2
		urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo"); // Pos20 TT1
		return urlsToBrowse;
	}

	public Set<String> getLocalhostLayout() {
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://localhost:4840/milo"); // Pos34 input station
		urlsToBrowse.add("opc.tcp://localhost:4841/milo"); // POS EAST of TT2, Pos 35 output station
		urlsToBrowse.add("opc.tcp://localhost:4842/milo"); // TT1 Pos20
		urlsToBrowse.add("opc.tcp://localhost:4843/milo"); // TT2 Pos21
		// virtual plotters
		urlsToBrowse.add("opc.tcp://localhost:4845/milo"); // POS NORTH of TT1 31
		urlsToBrowse.add("opc.tcp://localhost:4846/milo"); // POS NORTH of TT2 32
		return urlsToBrowse;
	}

	public Set<String> getTracingLocalhostLayout() {
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://localhost:4840/milo"); // Pos34 input station
		urlsToBrowse.add("opc.tcp://localhost:4842/milo"); // TT1 Pos20
		urlsToBrowse.add("opc.tcp://localhost:4843/milo"); // output station
		// virtual plotters
		urlsToBrowse.add("opc.tcp://localhost:4845/milo"); // POS NORTH of TT1 31
		urlsToBrowse.add("opc.tcp://localhost:4847/milo"); // POS NORTH of TT2 32
		return urlsToBrowse;
	}

	public Set<String> getTransportLayout() {
		Set<String> urlsToBrowse = new HashSet<String>();
		urlsToBrowse.add("opc.tcp://localhost:4840/milo"); // Pos34 input station
		urlsToBrowse.add("opc.tcp://localhost:4842/milo"); // Pos20 TT1
		urlsToBrowse.add("opc.tcp://localhost:4843/milo"); // Pos21 TT2
		urlsToBrowse.add("opc.tcp://localhost:4847/milo"); // Pos35 output station
		return urlsToBrowse;
	}

	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}

}
