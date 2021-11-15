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
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.mes.transport.msg.TransportModuleRequest;
import fiab.opcua.CapabilityImplementationMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestHandoverWithPlotEV3 {
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
        ProcessCore.Process p = ProduceProcess.getDemoProcess("P1-");
        OrderProcess op = new OrderProcess(p);
        op.activateProcess();
        step = op.getAvailableSteps().get(0);
    }

    @Test
    void testInputToTTAndPlotterWithDemoImage() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://192.168.0.34:4840"); //Pos34
        urlsToBrowse.add("opc.tcp://192.168.0.31:4840");    // POS NORTH31
        urlsToBrowse.add("opc.tcp://192.168.0.20:4842/milo");        // Pos20

        Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
        ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
        assertTrue(runTransport34to31TestWith(capURI2Spawning, urlsToBrowse));
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
                boolean sentPlotRequest = false;
                boolean plotterReady = false;
                while (machines.size() < urlsToBrowse.size() || doRun) {
                    TimedEvent te = expectMsgAnyClassOf(Duration.ofSeconds(300), MachineConnectedEvent.class,
                            IOStationStatusUpdateEvent.class, MachineStatusUpdateEvent.class, ReadyForProcessEvent.class);
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
                        } else if (msue.getStatus().equals(BasicMachineStates.IDLE)
                                && msue.getMachineId().contains("328")
                                && !sentPlotRequest) {
                            sendPlotRegister(machines.get(msue.getMachineId()).getAkkaActor(), getRef());
                            sentPlotRequest = true;
                        } else if (msue.getStatus().equals(BasicMachineStates.COMPLETE) || msue.getStatus().equals(BasicMachineStates.COMPLETING)) {
                            logger.info("Received COMPLETE/ING from: " + msue.getMachineId());
                        }if(plotterReady && msue.getStatus().equals(BasicMachineStates.STARTING) && msue.getMachineId().contains("328")){
                            //sendTransportRequestWest34ToNorth31(machines.get("192.168.0.20:4842/Turntable1/Turntable_FU"), getRef());
                            doRun = false;
                        }
                    }if (te instanceof ReadyForProcessEvent) {
                        assert(((ReadyForProcessEvent) te).isReady());
                        plotterReady = true;
                        logger.info("Sending Plot request");
                        sendPlotRequest(machines.get("opc.tcp://192.168.0.31:4840/328").getAkkaActor(), getRef());
                    }
                }
            }
        };
        return true;
    }


    private void sendPlotRequest(ActorRef plotter, ActorRef self) {
        LockForOrder lfo = new LockForOrder("Step1", "Order1");
        plotter.tell(lfo, self);
    }

    private void sendPlotRegister(ActorRef plotter, ActorRef self) {
        RegisterProcessStepRequest req = new RegisterProcessStepRequest("Order1", "Step1", step, self);
        plotter.tell(req, self);
    }


    private void logEvent(TimedEvent event) {
        logger.info(event.toString());
    }
}
