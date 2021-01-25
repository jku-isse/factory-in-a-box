package fiab.mes.mockactors.iostation.opcua;

import java.time.Duration;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.machine.actor.iostation.wrapper.IOStationOPCUAWrapper;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.mockactors.iostation.VirtualIOStationActorFactory;
import fiab.opcua.client.OPCUAClientFactory;

class TestIOStationOPCUAWrapper {

	private static final Logger logger = LoggerFactory.getLogger(TestIOStationOPCUAWrapper.class);
	
	InterMachineEventBus intraEventBus;
	AbstractCapability capability;
	Actor model;
	IOStationOPCUAWrapper wrapper;
	ActorRef machine;
	ActorRef machineEventBus;
	ActorSystem system;

	@BeforeEach
	void setup() throws Exception{
		system = ActorSystem.create("TEST_ROOT_SYSTEM");
		// assume OPCUA server (mock or otherwise is started
		NodeId capabilitImpl = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU");
		NodeId resetMethod = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/RESET");
		NodeId stopMethod = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/STOP");
		NodeId stateVar = NodeId.parse("ns=2;s=InputStation/HANDSHAKE_FU/STATE");
		OpcUaClient client = new OPCUAClientFactory().createClient("opc.tcp://localhost:4840/milo");
		client.connect().get();
		boolean isInputStation = true;
		capability = isInputStation ? IOStationCapability.getInputStationCapability() : IOStationCapability.getOutputStationCapability();
		intraEventBus = new InterMachineEventBus();
		machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		wrapper = new IOStationOPCUAWrapper(intraEventBus, client, capabilitImpl, stopMethod, resetMethod, stateVar, null);
		model = VirtualIOStationActorFactory.getDefaultIOStationActor(isInputStation, 34);
		

	}

	@Test
	void testReset() throws Exception {
		wrapper.reset();
	}

	@Test
	void testSubscribeState() throws InterruptedException {
		wrapper.subscribeToStatus();
		Thread.sleep(10000);
	}

	@Test
	void testActorIntegration() {
		new TestKit(system) { 
			{ 
				final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
				machine = system.actorOf(BasicIOStationActor.props(eventBusByRef, capability, model, wrapper, intraEventBus), model.getActorName());
				eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
				boolean doRun = true;
				int countConnEvents = 0;
				while ( countConnEvents < 1 || doRun) {
					TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class); 
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
