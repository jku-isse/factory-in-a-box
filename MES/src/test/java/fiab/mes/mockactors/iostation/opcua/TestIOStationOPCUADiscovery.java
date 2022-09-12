package fiab.mes.mockactors.iostation.opcua;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import fiab.functionalunit.connector.MachineEventBus;
import fiab.iostation.InputStationFactory;
import fiab.iostation.OutputStationFactory;
import fiab.mes.mockactors.iostation.VirtualIOStationActorFactory;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.ServerSideStates;
//import fiab.machine.iostation.opcua.StartupUtil;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.actor.iostation.wrapper.LocalIOStationActorSpawner;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.opcua.CapabilityImplementationMetadata;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;

class TestIOStationOPCUADiscovery {

	public static void main(String args[]) {
		//StartupUtil.startupInputstation(0, "VirtualInputStation1");
		//StartupUtil.startupOutputstation(1, "VirtualOutputStation1");
	}
		
	private static final Logger logger = LoggerFactory.getLogger(TestIOStationOPCUADiscovery.class);
	
//	InterMachineEventBus intraEventBus;
//	AbstractCapability capability;
//	Actor model;
//	IOStationOPCUAWrapper wrapper;
//	ActorRef machine;
	ActorRef machineEventBus;
	ActorSystem system;

	@BeforeEach
	void setup() throws Exception{
		system = ActorSystem.create("TEST_ROOT_SYSTEM");
		// assume OPCUA server (mock or otherwise is started
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	
	}

	@Test
	@Tag("SystemTest")
	void testDiscoveryActualInputStation() {
		discoverIOStation("opc.tcp://192.168.0.34:4840");
	}
	
	@Test
	@Tag("SystemTest")
	void testDiscoveryActualOutputStation() {
		discoverIOStation("opc.tcp://192.168.0.35:4840");
	}
	
	@Test
	@Tag("IntegrationTest")
	void testDiscoveryVirtualInputStation() {
		InputStationFactory.startInputStation(system, new MachineEventBus(), 4840, "InputStation");
		discoverIOStation("opc.tcp://localhost:4840");
	}

	@Test
	@Tag("IntegrationTest")
	void testDiscoveryVirtualOutputStation() {
		OutputStationFactory.startOutputStation(system, new MachineEventBus(), 4840, "OutputStation");
		discoverIOStation("opc.tcp://localhost:4840");
	}

	@Test
	@Tag("IntegrationTest")
	void testDiscoveryVirtualBothInputAndOutputStation() {
		//To automate testing we will spawn two virtual machines here
		InputStationFactory.startInputStation(system, new MachineEventBus(), 4840, "InputStation");
		OutputStationFactory.startOutputStation(system, new MachineEventBus(), 4841, "OutputStation");
		new TestKit(system) {
			{
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
				String endpointURL = "opc.tcp://localhost:4840";
				String endpointURL2 = "opc.tcp://localhost:4841";

				DefaultTransportPositionLookup lookup = new DefaultTransportPositionLookup();
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				capURI2Spawning.put(new AbstractMap.SimpleEntry<>(IOStationCapability.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED),
						context -> context.actorOf(LocalIOStationActorSpawner.props(lookup)));
				capURI2Spawning.put(new AbstractMap.SimpleEntry<>(IOStationCapability.OUTPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED),
						context -> context.actorOf(LocalIOStationActorSpawner.props(lookup)));
				ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());
				ActorRef discovAct2 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct2.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL2, capURI2Spawning), getRef());

				//boolean doRun = true;
				int countIdle = 0;
				int countConnEvents = 0;
				while (countConnEvents < 2 || countIdle < 2) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class);
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++;
					}
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED))
							getLastSender().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef());
					}
					if (te instanceof IOStationStatusUpdateEvent) {
						if (((IOStationStatusUpdateEvent) te).getStatus().equals(ServerSideStates.IDLE_LOADED)) {
							countIdle++;
						}
						if (((IOStationStatusUpdateEvent) te).getStatus().equals(ServerSideStates.IDLE_EMPTY)) {
							countIdle++;
						}
					}
				}
			}};
	}
	
	private void discoverIOStation(String endpointURL) {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
				DefaultTransportPositionLookup lookup = new DefaultTransportPositionLookup();
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(IOStationCapability.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props(lookup));
					}
				});
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(IOStationCapability.OUTPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props(lookup));
					}
				});
				ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());			
				
				//boolean doRun = true;
				int countIdle = 0;
				int countConnEvents = 0;
				while (countConnEvents < 1 || countIdle < 1) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
					}
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(BasicMachineStates.STOPPED)) 
							getLastSender().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef());
					}
					if (te instanceof IOStationStatusUpdateEvent) {
						if (((IOStationStatusUpdateEvent) te).getStatus().equals(ServerSideStates.IDLE_LOADED)) {
							countIdle++;
						}
						if (((IOStationStatusUpdateEvent) te).getStatus().equals(ServerSideStates.IDLE_EMPTY)) {
							countIdle++;
						}
					}
				}
			}};
	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}


}
