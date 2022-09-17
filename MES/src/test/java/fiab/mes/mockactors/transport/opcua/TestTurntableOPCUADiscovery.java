package fiab.mes.mockactors.transport.opcua;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
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
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.opcua.CapabilityImplementationMetadata;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.turntable.TurntableFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import testutils.PortUtils;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

class TestTurntableOPCUADiscovery {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("ROOT_SYSTEM_TURNTABLE_OPCUA");
        int portOffset = 2;
        boolean exposeInternalControls = false;
        //system.actorOf(OPCUATurntableRootActor.props("Turntable1", portOffset, exposeInternalControls), "TurntableRoot");
        TurntableFactory.startStandaloneTurntable(system, 4840 + portOffset, "TurntableRoot");
    }

    private static final Logger logger = LoggerFactory.getLogger(TestTurntableOPCUADiscovery.class);

    //	InterMachineEventBus intraEventBus;
//	AbstractCapability capability;
//	Actor model;
//	IOStationOPCUAWrapper wrapper;
//	ActorRef machine;
    ActorRef machineEventBus;
    ActorSystem system;

    @BeforeEach
    void setup() throws Exception {
        system = ActorSystem.create("TEST_ROOT_SYSTEM");
        // assume OPCUA server (mock or otherwise is started
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @Test
    @Tag("IntegrationTest")
    void testDiscoveryVirtualTurntable() {
        int turntablePort = PortUtils.findNextFreePort();
        TurntableFactory.startStandaloneTurntable(system, turntablePort, "TurntableRoot");
        discoverTurntable("opc.tcp://localhost:" + turntablePort);
    }

    @Test
    @Tag("SystemTest")
    void testDiscoveryTurntableNiryo() {
        discoverTurntable("opc.tcp://192.168.0.40:4842");
    }

    @Test
    @Tag("SystemTest")
    void testDiscoveryActualTurntable() {
        discoverTurntable("opc.tcp://192.168.0.20:4842");
    }

    private void discoverTurntable(String endpointURL) {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());
                // setup discoveryactor


                Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
                capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {
                    @Override
                    public ActorRef createActorSpawner(ActorContext context) {
                        return context.actorOf(LocalTransportModuleActorSpawner.props(new DefaultTransportPositionLookup()));
                    }
                });
                ActorRef discovAct = system.actorOf(CapabilityDiscoveryActor.props());
                discovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());

                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                boolean doRun = true;
                int countConnEvents = 0;
                while (machines.size() < 1 || doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class);
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
            }
        };
    }


    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }


}
