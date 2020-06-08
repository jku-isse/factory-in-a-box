package fiab.mes.mockactors.plotter.opcua;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.plotting.WellknownPlotterCapability.SupportedColors;
import fiab.machine.plotter.opcua.StartupUtil;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;

class TestPlotterOPCUADiscovery {

	public static void main(String args[]) {
		StartupUtil.startup(0, "VirtualPlotter1", SupportedColors.BLACK);
	}
	
	public static String TESTPLOTTER31 = "opc.tcp://localhost:4840/milo";
	public static String ACTUALPLOTTER31 = "opc.tcp://192.168.0.31:4840";
	
	private static final Logger logger = LoggerFactory.getLogger(TestPlotterOPCUADiscovery.class);
	
//	InterMachineEventBus intraEventBus;
//	AbstractCapability capability;
//	Actor model;
//	IOStationOPCUAWrapper wrapper;
//	ActorRef machine;
	ActorRef machineEventBus;
	ActorSystem system;

	@BeforeEach
	void setup() throws Exception{
		system = ActorSystem.create("TEST_PLOTTER_ROOT_SYSTEM");
		//StartupUtil.startup(0, "TestPlotter", SupportedColors.BLACK);
		// assume OPCUA server (mock or otherwise is started
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
	
	}

	@Test
	void testDiscoveryIntegrationVirtualPlotter() {
		String endpointURL = TESTPLOTTER31;
		runDiscovery(endpointURL);
	}
	
	@Test
	void testDiscoveryIntegrationActualPlotter() {
		String endpointURL = ACTUALPLOTTER31;
		runDiscovery(endpointURL);
	}
	
	private void runDiscovery(String endpointURL) {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
				
				
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				ShopfloorConfigurations.addColorPlotterStationSpawner(capURI2Spawning);			
//				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownPlotterCapability.PLOTTING_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
//					@Override
//					public ActorRef createActorSpawner(ActorContext context) {
//						return context.actorOf(LocalPlotterActorSpawner.props());
//					}
//				});
				ActorRef discovAct = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());
				
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();				

				boolean doRun = true;
				int countConnEvents = 0;
				while (countConnEvents < 1 || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
						machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
					}
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(BasicMachineStates.STOPPED)) { 							
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						}
						else if (msue.getStatus().equals(BasicMachineStates.IDLE)) {							
							logger.info("Completing test upon receiving IDLE from: "+msue.getMachineId());
							doRun = false;
						}
					}

				}
			}};
	}
	
	
	@Test //MANUAL TEST!!!, requires manually stopping and rebooting of (virtual) plotter
	void testConnectionInterruption() {
		String endpointURL = TESTPLOTTER31;
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
				
				
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				ShopfloorConfigurations.addColorPlotterStationSpawner(capURI2Spawning);			
//				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownPlotterCapability.PLOTTING_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
//					@Override
//					public ActorRef createActorSpawner(ActorContext context) {
//						return context.actorOf(LocalPlotterActorSpawner.props());
//					}
//				});
				ActorRef discovAct = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());
				
				HashMap<String,AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();				

				boolean doRun = true;
				int countConnEvents = 0;
				while (countConnEvents < 1 || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), TimedEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
						logger.info("Machine Connected: "+((MachineConnectedEvent) te).getMachineId());
						machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
					}
					if (te instanceof MachineStatusUpdateEvent) {
						MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
						if (msue.getStatus().equals(BasicMachineStates.STOPPED)) { 														
							machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());							
						}
						else if (msue.getStatus().equals(BasicMachineStates.COMPLETE)) {							
							logger.info("Completing test upon receiving COMPLETE from: "+msue.getMachineId());
							doRun = false;
						}
					}

				}
			}};
	}
	
	private void logEvent(TimedEvent event) {
		logger.info(event.toString());
	}


}
