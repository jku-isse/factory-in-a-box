package fiab.mes.mockactors.transport.opcua;

import ProcessCore.ProcessStep;
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
import fiab.mes.order.OrderProcess;
import fiab.mes.order.ecore.ProduceProcess;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.opcua.CapabilityImplementationMetadata;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

public class TestEV3TurntableWithIOStations {
    private static final Logger logger = LoggerFactory.getLogger(TestTurntableWithIOStations.class);

    ActorRef machineEventBus;
    static ActorSystem system;
    ProcessStep step;

    @BeforeAll
    static void initSystem() {
        system = ActorSystem.create("EV3TurntableTest-System");
    }

    @BeforeEach
    void setup() throws Exception {
        system = ActorSystem.create("TEST_ROOT_SYSTEM");
        // assume OPCUA server (mock or otherwise is started
        machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ProcessCore.Process p = ProduceProcess.getSequential4ColorProcess("P1-");
        OrderProcess op = new OrderProcess(p);
        op.activateProcess();
        step = op.getAvailableSteps().get(0);
    }

    @Test
    void InputandTTandPlotter() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34
        urlsToBrowse.add("opc.tcp://192.168.0.31:4840");    // POS NORTH31
        urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");        // Pos20

        Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
        ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
        Assert.assertTrue(runTransport34to31TestWith(capURI2Spawning, urlsToBrowse));
    }

    //Not working
    @Test
    void InputandTTandOutput() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34
        urlsToBrowse.add("opc.tcp://192.168.0.35:4840");    // POS EAST 35 (will be 37)
        urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");        // Pos20

        Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
        ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
        Assert.assertTrue(runTransport34to35TestWith(capURI2Spawning, urlsToBrowse));
    }

    private boolean runTransport34to31TestWith(Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning, Set<String> urlsToBrowse) {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                urlsToBrowse.forEach(url -> {
                    ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
                    discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
                });

                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                boolean didReactOnIdle = false;
                boolean doRun = true;
                while (machines.size() < urlsToBrowse.size() || doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                        machines.get(((MachineConnectedEvent) te).getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Stop(((MachineConnectedEvent) te).getMachineId()), getRef());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE)
                                && msue.getMachineId().contains("Turntable")
                                && !didReactOnIdle) {
                            logger.info("Sending TEST transport request to: " + msue.getMachineId());
                            TransportModuleRequest req = new TransportModuleRequest(machines.get(msue.getMachineId()), new TransportRoutingInterface.Position("34"), new TransportRoutingInterface.Position("31"), "Order1", "TReq1");
                            machines.get(msue.getMachineId()).getAkkaActor().tell(req, getRef());
                            didReactOnIdle = true;
                        } else if (msue.getStatus().equals(BasicMachineStates.COMPLETE) || msue.getStatus().equals(BasicMachineStates.COMPLETING)) {
                            logger.info("Completing test upon receiving COMPLETE/ING from: " + msue.getMachineId());
                            doRun = false;
                        }
                    }
                }
            }
        };
        return true;
    }

    private boolean runTransport34to35TestWith(Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning, Set<String> urlsToBrowse) {
        new TestKit(system) {
            {
                final ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
                eventBusByRef.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef());

                urlsToBrowse.forEach(url -> {
                    ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
                    discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), getRef());
                });

                HashMap<String, AkkaActorBackedCoreModelAbstractActor> machines = new HashMap<>();

                boolean didReactOnIdle = false;
                boolean doRun = true;
                while (machines.size() < urlsToBrowse.size() || doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class, IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class);
                    logEvent(te);
                    if (te instanceof MachineConnectedEvent) {
                        machines.put(((MachineConnectedEvent) te).getMachineId(), ((MachineConnectedEvent) te).getMachine());
                        //machines.get(((MachineConnectedEvent) te).getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Stop(((MachineConnectedEvent) te).getMachineId()), getRef());
                    }
                    if (te instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) te;
                        if (msue.getStatus().equals(BasicMachineStates.STOPPED)) {
                            machines.get(msue.getMachineId()).getAkkaActor().tell(new GenericMachineRequests.Reset(msue.getMachineId()), getRef());
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE)
                                && msue.getMachineId().contains("Turntable")
                                && !didReactOnIdle) {
                            logger.info("Sending TEST transport request to: " + msue.getMachineId());
                            TransportModuleRequest req = new TransportModuleRequest(machines.get(msue.getMachineId()), new TransportRoutingInterface.Position("34"), new TransportRoutingInterface.Position("35"), "Order1", "TReq1");
                            machines.get(msue.getMachineId()).getAkkaActor().tell(req, getRef());
                            didReactOnIdle = true;
                        } else if (msue.getStatus().equals(BasicMachineStates.COMPLETE) || msue.getStatus().equals(BasicMachineStates.COMPLETING)) {
                            logger.info("Completing test upon receiving COMPLETE/ING from: " + msue.getMachineId());
                            doRun = false;
                        }
                    }
                }
            }
        };
        return true;
    }

    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }
}
