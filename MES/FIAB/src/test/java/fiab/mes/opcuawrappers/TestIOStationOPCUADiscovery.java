package fiab.mes.opcuawrappers;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.TimedEvent;
import fiab.mes.machine.actor.iostation.wrapper.LocalIOStationActorSpawner;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.opcua.CapabilityImplementationMetadata;
import fiab.mes.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

class TestIOStationOPCUADiscovery {

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
//		NodeId capabilitImpl = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU");
//		NodeId resetMethod = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/RESET");
//		NodeId stopMethod = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/STOP");
//		NodeId stateVar = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/STATE");
//		OpcUaClient client = new OPCUAUtils().createClient("opc.tcp://localhost:4840/milo");
//		client.connect().get();
//		boolean isInputStation = true;
//		capability = isInputStation ? HandshakeProtocol.getInputStationCapability() : HandshakeProtocol.getOutputStationCapability();
//		intraEventBus = new InterMachineEventBus();
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
//		wrapper = new IOStationOPCUAWrapper(intraEventBus, client, capabilitImpl, stopMethod, resetMethod, stateVar);
//		model = MockIOStationFactory.getDefaultIOStationActor(isInputStation, 34);
		

	}

	@Test
	void testDiscoveryIntegration() {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);				
				eventBusByRef.tell(new SubscribeMessage(getRef(), new SubscriptionClassifier("Tester", "*")), getRef() );
				// setup discoveryactor
				String endpointURL = "opc.tcp://localhost:4840/milo";
				
				Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(HandshakeProtocol.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {					
					@Override
					public ActorRef createActorSpawner(ActorContext context) {
						return context.actorOf(LocalIOStationActorSpawner.props());
					}
				});
				ActorRef discovAct = system.actorOf(CapabilityDiscoveryActor.props());
				discovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());
				
				boolean doRun = true;
				int countConnEvents = 0;
				while (countConnEvents < 1 || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
					logEvent(te);
					if (te instanceof MachineConnectedEvent) {
						countConnEvents++; 
					}
					if (te instanceof MachineStatusUpdateEvent) {
						if (((MachineStatusUpdateEvent) te).getStatus().equals(MachineStatus.STOPPED)) 
							getLastSender().tell(new GenericMachineRequests.Reset(((MachineStatusUpdateEvent) te).getMachineId()), getRef());
					}
					if (te instanceof IOStationStatusUpdateEvent) {
						if (((IOStationStatusUpdateEvent) te).getStatus().equals(ServerSide.IdleLoaded)) {
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
