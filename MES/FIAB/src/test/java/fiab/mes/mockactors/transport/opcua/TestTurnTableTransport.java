package fiab.mes.mockactors.transport.opcua;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.iostation.wrapper.LocalIOStationActorSpawner;
import fiab.mes.machine.actor.plotter.wrapper.LocalPlotterActorSpawner;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.transport.actor.transportmodule.wrapper.LocalTransportModuleActorSpawner;
import fiab.opcua.CapabilityImplementationMetadata;
import fiab.turntable.actor.InternalTransportModuleRequest;
import fiab.turntable.opcua.methods.TransportRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class TestTurnTableTransport {
    private static final Logger logger = LoggerFactory.getLogger(TestTurntableOPCUADiscovery.class);

    //	InterMachineEventBus intraEventBus;
//	AbstractCapability capability;
//	Actor model;
//	IOStationOPCUAWrapper wrapper;
//	ActorRef machine;
    static ActorRef machineEventBus;
    static ActorSystem system;

    @BeforeAll
    static void setup() throws Exception{
        system = ActorSystem.create("TEST_ROOT_SYSTEM");
        // assume OPCUA server (mock or otherwise is started
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);

    }

    @Test
    public void testDiscoveryIntegration() {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
                // setup discoveryactor
                String ttEndpointURL = "opc.tcp://192.168.0.20:4842/milo";
                String inputEndpointURL = "opc.tcp://192.168.0.34:4840";
                String plotterNorthEndpointURL = "opc.tcp://192.168.0.31";
                //String endpointURL = "opc.tcp://localhost:4842/milo";
                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
                capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {
                    @Override
                    public ActorRef createActorSpawner(ActorContext context) {
                        return context.actorOf(LocalTransportModuleActorSpawner.props());
                    }
                });
                capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(IOStationCapability.INPUTSTATION_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {
                    @Override
                    public ActorRef createActorSpawner(ActorContext context) {
                        return context.actorOf(LocalIOStationActorSpawner.props());
                    }
                });
                capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownPlotterCapability.PLOTTING_CAPABILITY_BASE_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {
                    @Override
                    public ActorRef createActorSpawner(ActorContext context) {
                        return context.actorOf(LocalPlotterActorSpawner.props());
                    }
                });
                ActorRef ttDiscovAct = system.actorOf(CapabilityDiscoveryActor.props());
                ttDiscovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(ttEndpointURL, capURI2Spawning), getRef());
                ActorRef iDiscovAct = system.actorOf(CapabilityDiscoveryActor.props());
                iDiscovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(inputEndpointURL, capURI2Spawning), getRef());
                ActorRef plotDiscovAct = system.actorOf(CapabilityDiscoveryActor.props());
                plotDiscovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(plotterNorthEndpointURL, capURI2Spawning), getRef());
                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                boolean doRun = true;
                int countConnEvents = 0;
                while (machines.size() < 3 || doRun) {
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
                        if (msue.getStatus().equals(BasicMachineStates.IDLE) && msue.getMachineId().equals("Turntable1/Turntable_FU/STATE")){
                            //doRun = false;
                            logger.info("TT is idle, firing InternalTransportModuleRequest");
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new InternalTransportModuleRequest(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_CLIENT, "TestOrder1", "Req1"), getRef());
                        }
                        if(msue.getStatus().equals(BasicMachineStates.COMPLETE)){
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
