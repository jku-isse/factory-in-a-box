package fiab.mes.mockactors.transport;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import fiab.core.capabilities.transport.TransportRequest;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.shopfloor.layout.DefaultTestLayout;
import fiab.mes.shopfloor.layout.ShopfloorLayout;
import fiab.mes.shopfloor.layout.SingleTurntableLayout;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import fiab.core.capabilities.BasicMachineStates;
import fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import fiab.core.capabilities.events.TimedEvent;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.mes.mockactors.iostation.VirtualIOStationActorFactory;
import fiab.mes.order.OrderProcess;
import fiab.mes.shopfloor.DefaultLayout;

import static fiab.mes.shopfloor.utils.ShopfloorUtils.*;

@Tag("IntegrationTest")
public class TestTransportModuleCoordinatorActor {

    protected ActorSystem system;
    protected ActorRef machine;
    private ActorRef interMachineEventBus;
    public final String ROOT_SYSTEM = "TEST_TTINTEGRATION";

    private static final Logger logger = LoggerFactory.getLogger(TestTransportModuleCoordinatorActor.class);

    @BeforeEach
    public void setup() throws Exception {
        system = ActorSystem.create(ROOT_SYSTEM);
        interMachineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    void testSetupMinimalShopfloor() {
        new TestKit(system) {
            {
                ShopfloorLayout layout = new SingleTurntableLayout(system, interMachineEventBus);
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                layout.initializeAndDiscoverParticipantsForId(getRef(), INPUT_STATION, OUTPUT_STATION, TURNTABLE_1);
                boolean hasSentReq = false;
                boolean doRun = true;
                String turntableId = layout.getParticipantForId(TURNTABLE_1).getProxyMachineId();
                while (doRun) {
                    TimedEvent mue = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineUpdateEvent.class, MachineConnectedEvent.class);
                    logEvent(mue);
                    if (mue instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent machineStatusUpdateEvent = ((MachineStatusUpdateEvent) mue);
                        if (machineStatusUpdateEvent.getStatus().equals(BasicMachineStates.IDLE) && !hasSentReq &&
                                machineStatusUpdateEvent.getMachineId().equals(turntableId)) {
                            layout.getParticipantForId(TURNTABLE_1).getRemoteMachine().orElseThrow().tell(new TransportRequest(
                                    TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT,
                                    TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT,
                                    "TestOrder1", "Req1"), getRef());
                            hasSentReq = true;
                        }
                        if (hasSentReq && ((MachineStatusUpdateEvent) mue).getStatus().equals(BasicMachineStates.COMPLETE)) {
                            doRun = false;
                        }
                    }
                }
            }
        };
    }


    @Test
    void testSetup2TTplusIO() {
        new TestKit(system) {
            {
                ShopfloorLayout layout = new DefaultTestLayout(system, interMachineEventBus);
                layout.subscribeToInterMachineEventBus(getRef(), "Tester");
                layout.initializeAndDiscoverParticipantsForId(getRef(), INPUT_STATION, OUTPUT_STATION, TURNTABLE_1, TURNTABLE_2);

                String turntable1Id = layout.getParticipantForId(TURNTABLE_1).getProxyMachineId();
                String turntable2Id = layout.getParticipantForId(TURNTABLE_2).getProxyMachineId();

                boolean hasSentReqTT1 = false;
                boolean hasSentReqTT2 = false;
                boolean doRun = true;
                while (doRun) {
                    TimedEvent mue = expectMsgAnyClassOf(Duration.ofSeconds(30), MachineUpdateEvent.class, MachineConnectedEvent.class);
                    logEvent(mue);
                    if (mue instanceof MachineStatusUpdateEvent) {
                        MachineStatusUpdateEvent msue = (MachineStatusUpdateEvent) mue;
                        if (msue.getMachineId().equals(turntable1Id) && msue.getStatus().equals(BasicMachineStates.IDLE) && !hasSentReqTT1) {
                            ActorRef turntable1 = layout.getParticipantForId(TURNTABLE_1).getRemoteMachine().orElseThrow();
                            turntable1.tell(new TransportRequest(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER, "TestOrder1", "Req1"), getRef());
                            hasSentReqTT1 = true;
                        }
                        if (msue.getMachineId().equals(turntable2Id) && msue.getStatus().equals(BasicMachineStates.IDLE) && !hasSentReqTT2) {
                            ActorRef turntable2 = layout.getParticipantForId(TURNTABLE_2).getRemoteMachine().orElseThrow();
                            turntable2.tell(new TransportRequest(TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_CLIENT, TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_CLIENT, "TestOrder2", "Req2"), getRef());
                            hasSentReqTT2 = true;
                        }
                        if (msue.getMachineId().equals(turntable2Id) && msue.getStatus().equals(BasicMachineStates.COMPLETE)) {
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


}
