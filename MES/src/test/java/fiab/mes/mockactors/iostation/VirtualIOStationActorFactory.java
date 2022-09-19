package fiab.mes.mockactors.iostation;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.iostation.InputStationFactory;
import fiab.iostation.OutputStationFactory;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.machine.actor.iostation.wrapper.IOStationOPCUAWrapper;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import testutils.PortUtils;

public class VirtualIOStationActorFactory {

    public static VirtualIOStationActorFactory getInputStationForPosition(ActorSystem system, ActorSelection eventBusByRef, boolean doAutoReload, int ipId) {
        return new VirtualIOStationActorFactory(system, true, eventBusByRef, doAutoReload, ipId);
    }

    public static VirtualIOStationActorFactory getOutputStationForPosition(ActorSystem system, ActorSelection eventBusByRef, boolean doAutoReload, int ipId) {
        return new VirtualIOStationActorFactory(system, false, eventBusByRef, doAutoReload, ipId);
    }

    public static String WRAPPER_POSTFIX = "Wrapper";

    public ActorRef machine;
    public ActorRef proxy;
    public MachineEventBus intraEventBus;
    public AbstractCapability capability;
    public Actor model;
    public int opcUaPort;
    public String machineName;
    //private static AtomicInteger actorCount = new AtomicInteger();
    public static boolean doAutoComplete = true;

    private VirtualIOStationActorFactory(ActorSystem system, boolean isInputStation, ActorSelection eventBusByRef, boolean doAutoReload, int ipId) {
        model = VirtualIOStationActorFactory.getDefaultIOStationActor(isInputStation, ipId);
        String actorName = model.getActorName();
        machineName = isInputStation ? "InputStation" + ipId : "OutputStation" + ipId;
        opcUaPort = PortUtils.findNextFreePort();

        if (isInputStation) {
            capability = IOStationCapability.getInputStationCapability();
            machine = InputStationFactory.startStandaloneInputStation(system, opcUaPort, machineName);
        } else {
            machineName = "OutputStation";
            capability = IOStationCapability.getOutputStationCapability();
            machine = OutputStationFactory.startStandaloneOutputStation(system, opcUaPort, machineName);
        }
        NodeId capabilityImpl = NodeId.parse("ns=2;s=" + machineName + "/HANDSHAKE_FU");
        NodeId resetMethod = NodeId.parse("ns=2;s=" + machineName + "/HANDSHAKE_FU/RESET");
        NodeId stopMethod = NodeId.parse("ns=2;s=" + machineName + "/HANDSHAKE_FU/STOP");
        NodeId stateVar = NodeId.parse("ns=2;s=" + machineName + "/HANDSHAKE_FU/STATE");

        // assume OPCUA server (mock or otherwise is started
        intraEventBus = new MachineEventBus();
        FiabOpcUaClient client;
        try {
            client = OPCUAClientFactory.createFIABClientAndConnect("opc.tcp://127.0.0.1:" + opcUaPort);
            model = VirtualIOStationActorFactory.getDefaultIOStationActor(isInputStation, ipId);
            IOStationOPCUAWrapper opcuaWrapper = new IOStationOPCUAWrapper(intraEventBus, client, capabilityImpl, stopMethod, resetMethod, stateVar, null);
            proxy = system.actorOf(BasicIOStationActor.props(eventBusByRef, capability, model, opcuaWrapper, intraEventBus), model.getActorName()+WRAPPER_POSTFIX);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Actor getDefaultIOStationActor(boolean isInputStation, int id) {
        //int id = actorCount.getAndIncrement();
        //int id = isInputStation ? 34 : 35; //at IP/Pos 34 is inputstation, at IP/Pos3 35 is outputstation
        String type = isInputStation ? "Input" : "Output";
        Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
        actor.setID(type + "StationActor" + id);
        actor.setActorName(type + "StationActor" + id);
        actor.setDisplayName(type + "Actor" + id);
        actor.setUri("http://192.168.0." + id + "/" + type + "Actor" + id);
        return actor;
    }

    /*public static class ParentActor extends AbstractActor {

        private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

        boolean doAutoReload;
        boolean isInputStation;
        ActorRef child;
        ActorRef self;
        MachineEventBus bus;

        static public Props props(boolean isInputStation, boolean doAutoReload, MachineEventBus bus) {
            return Props.create(ParentActor.class, () -> new ParentActor(isInputStation, doAutoReload, bus));
        }

        public ParentActor(boolean isInputStation, boolean doAutoReload, MachineEventBus bus) {
            this.isInputStation = isInputStation;
            this.doAutoReload = doAutoReload;
            this.bus = bus;
            this.self = getSelf();
        }

        @Override
        public Receive createReceive() {

            return receiveBuilder()
                    .match(ServerSideStates.class, req -> {
                        log.info(req.toString());
                        bus.publish(new IOStationStatusUpdateEvent("", "Mock Endpoint has new State", req));
                        if (req.equals(ServerSideStates.COMPLETE) && doAutoReload) { //we auto reload here
                            child = getSender();
                            reloadPallet();
                        }
                    })
                    //.match(LocalEndpointStatus.LocalServerEndpointStatus.class, les -> {//ignore
                    //	})
                    .build();
        }

        private void reloadPallet() {
            //tell handshake that the pallet is loaded if inputstation, otherwise setempty
            context().system()
                    .scheduler()
                    .scheduleOnce(Duration.ofMillis(1000),
                            new Runnable() {
                                @Override
                                public void run() {
                                    if (isInputStation) {
                                        child.tell(HandshakeCapability.StateOverrideRequests.SetLoaded, self);
                                    } else {
                                        child.tell(HandshakeCapability.StateOverrideRequests.SetEmpty, self);
                                    }
                                }
                            }, context().system().dispatcher());
        }

    }*/


}
