package fiab.mes.mockactors.plotter.opcua;

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
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.opcua.CapabilityImplementationMetadata.ProvOrReq;
import fiab.plotter.PlotterFactory;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPlotterOPCUADiscovery {

    public static void main(String args[]) {
        StartupUtil.startup(0, "VirtualPlotter1", SupportedColors.BLACK);
    }

    //public static String TESTPLOTTER31 = "opc.tcp://localhost:4840/milo";
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
    void setup() throws Exception {
        system = ActorSystem.create("TEST_PLOTTER_ROOT_SYSTEM");
        //StartupUtil.startup(0, "TestPlotter", SupportedColors.BLACK);
        // assume OPCUA server (mock or otherwise is started
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);

    }

    @Test
    @Tag("IntegrationTest")
    void testDiscoveryIntegrationVirtualPlotter() {
        new TestKit(system) {
            {
                //Set up virtual plotter
                int plotterPort = PortUtils.findNextFreePort();
                PlotterFactory.startStandalonePlotter(system, plotterPort, "TestPlotter", SupportedColors.BLACK);
                String endpointURL = "opc.tcp://localhost:" + plotterPort;
                //subscribe to eventbus
                final ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());
                //Set up discoveryActor
                Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<>();
                ShopfloorConfigurations.addColorPlotterStationSpawner(capURI2Spawning, new LocalPlotterTestPositionLookup());
                ActorRef discovAct = system.actorOf(CapabilityDiscoveryActor.props());
                discovAct.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointURL, capURI2Spawning), getRef());
                //check if connected and in initial state
                MachineConnectedEvent connectedEvent = expectMsgClass(MachineConnectedEvent.class);
                MachineStatusUpdateEvent statusUpdateEvent = expectMsgClass(MachineStatusUpdateEvent.class);
                assertEquals(BasicMachineStates.STOPPED, statusUpdateEvent.getStatus());
                //check reset successful
                connectedEvent.getMachine().getAkkaActor().tell(new GenericMachineRequests.Reset(getRef().path().name()), getRef());
                statusUpdateEvent = expectMsgClass(MachineStatusUpdateEvent.class);
                assertEquals(BasicMachineStates.RESETTING, statusUpdateEvent.getStatus());
                statusUpdateEvent = expectMsgClass(MachineStatusUpdateEvent.class);
                assertEquals(BasicMachineStates.RESETTING, statusUpdateEvent.getStatus());  //Where does this second reset come from?
                statusUpdateEvent = expectMsgClass(MachineStatusUpdateEvent.class);
                assertEquals(BasicMachineStates.IDLE, statusUpdateEvent.getStatus());
            }
        };
    }

    @Test
    @Tag("SystemTest")
    void testDiscoveryIntegrationActualPlotter() {
        String endpointURL = ACTUALPLOTTER31;
        runDiscovery(endpointURL);
    }

    private void runDiscovery(String endpointURL) {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());
                // setup discoveryactor


                Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
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
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        countConnEvents++;
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE)) {
                            logger.info("Completing test upon receiving IDLE from: " + msue.getMachineId());
                            doRun = false;
                        }
                    }

                }
            }
        };
    }


    @Test //MANUAL TEST!!!, requires manually stopping and rebooting of (virtual) plotter
    @Tag("SystemTest")
    void testConnectionInterruption() {
        String endpointURL = ACTUALPLOTTER31;
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());
                // setup discoveryactor


                Map<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
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
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(30), TimedEvent.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        countConnEvents++;
                        logger.info("Machine Connected: " + ((MachineConnectedEvent) te).getMachineId());
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.COMPLETE)) {
                            logger.info("Completing test upon receiving COMPLETE from: " + msue.getMachineId());
                            doRun = false;
                        }
                    }

                }
            }
        };
    }

    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }

    class LocalPlotterTestPositionLookup implements TransportPositionLookupInterface, TransportPositionParser {

        private final HashMap<TransportRoutingInterface.Position, AkkaActorBackedCoreModelAbstractActor> lookupTable = new HashMap<>();

        @Override
        public TransportRoutingInterface.Position getPositionForActor(AkkaActorBackedCoreModelAbstractActor actor) {
            TransportRoutingInterface.Position pos = parseLastIPPos(actor.getModelActor().getUri());
            if (pos != TransportRoutingInterface.UNKNOWN_POSITION)
                lookupTable.put(pos, actor);
            return pos;
        }

        @Override
        public Optional<AkkaActorBackedCoreModelAbstractActor> getActorForPosition(TransportRoutingInterface.Position pos) {
            return Optional.ofNullable(lookupTable.get(pos));
        }

        @Override
        public TransportRoutingInterface.Position parsePosViaPortNr(String uriAsString) {
            return new TransportRoutingInterface.Position("34");
        }

        @Override
        public TransportRoutingInterface.Position parseLastIPPos(String uriAsString) {
            return new TransportRoutingInterface.Position("34");
        }
    }


}
