package fiab.mes.mockactors.transport.opcua;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import main.java.fiab.core.capabilities.BasicMachineStates;
import main.java.fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import main.java.fiab.core.capabilities.events.TimedEvent;
import main.java.fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.transport.actor.transportmodule.wrapper.LocalTransportModuleActorSpawner;
import fiab.opcua.CapabilityImplementationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class TestEV3TurntableOPCUADiscovery {

    private static final Logger logger = LoggerFactory.getLogger(TestTurntableOPCUADiscovery.class);

    ActorRef machineEventBus;
    ActorSystem system;

    @BeforeEach
    void setup() throws Exception{
        system = ActorSystem.create("TEST_ROOT_SYSTEM");
        // assume OPCUA server (mock or otherwise is started
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @Test
    void testDiscoveryIntegration() {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
                // setup discoveryactor
                String endpointURL = "opc.tcp://192.168.0.20:4842/milo";

                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
                capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {
                    @Override
                    public ActorRef createActorSpawner(ActorContext context) {
                        return context.actorOf(LocalTransportModuleActorSpawner.props());
                    }
                });
                ActorRef discovAct = system.actorOf(CapabilityDiscoveryActor.props());
                discovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());

                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                boolean doRun = true;
                int countConnEvents = 0;
                while (machines.size() < 1 || doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        }
                        if (msue.getStatus().equals(BasicMachineStates.IDLE))
                            doRun = false;
                    }

                }
            }};
    }



    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }


}
