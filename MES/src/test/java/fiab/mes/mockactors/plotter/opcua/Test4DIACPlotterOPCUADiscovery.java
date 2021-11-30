package fiab.mes.mockactors.plotter.opcua;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.opcua.CapabilityImplementationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class Test4DIACPlotterOPCUADiscovery {

    private static final Logger logger = LoggerFactory.getLogger(Test4DIACPlotterOPCUADiscovery.class);

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
    void testDiscoveryIntegrationPlotterPos31() {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
                // setup discoveryactor
                String endpointURL = "opc.tcp://192.168.0.31:4840";

                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
                ShopfloorConfigurations.addColorPlotterStationSpawner(capURI2Spawning, new DefaultTransportPositionLookup());
//				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownPlotterCapability.PLOTTING_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {
//					@Override
//					public ActorRef createActorSpawner(ActorContext context) {
//						return context.actorOf(LocalPlotterActorSpawner.props());
//					}
//				});
                ActorRef discovAct = system.actorOf(CapabilityDiscoveryActor.props());
                discovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());

                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

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

   /* @Test
    void testDiscoveryIntegrationPlotterPos37() {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );
                // setup discoveryactor
                String endpointURL = "opc.tcp://192.168.0.37:4840";

                Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
                ShopfloorConfigurations.addColorPlotterStationSpawner(capURI2Spawning);
//				capURI2Spawning.put(new AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>(WellknownPlotterCapability.PLOTTING_CAPABILITY_URI, CapabilityImplementationMetadata.ProvOrReq.PROVIDED), new CapabilityCentricActorSpawnerInterface() {
//					@Override
//					public ActorRef createActorSpawner(ActorContext context) {
//						return context.actorOf(LocalPlotterActorSpawner.props());
//					}
//				});
                ActorRef discovAct = system.actorOf(CapabilityDiscoveryActor.props());
                discovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());

                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

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

*/
    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }

}
