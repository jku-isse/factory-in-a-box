package fiab.mes.shopfloor.participants.virtual;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.iostation.OutputStationFactory;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.machine.actor.iostation.wrapper.IOStationOPCUAWrapper;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import testutils.PortUtils;

import java.util.Map;

import static fiab.mes.shopfloor.participants.ParticipantInfo.WRAPPER_POSTFIX;
import static fiab.mes.shopfloor.participants.ParticipantInfo.localhostOpcUaPrefix;

public class VirtualOutputStationFactory {

    /**
     * Starts a new outputStation instance at an arbitrary free opcua port and a given position on the shopfloor
     * Then a proxy for machine is spawned that can be used to communicate events
     *
     * @param system The actor system
     * @param positionMap mapping of unique machine name/id to position
     * @return proxy as an actorRef for virtual machine
     */
    public static ParticipantInfo initOutputStationAndReturnInfo(ActorSystem system, String machineId, Map<String, Position> positionMap) {
        int opcUaPort = PortUtils.findNextFreePort();
        Position position = positionMap.getOrDefault(machineId, TransportRoutingInterface.UNKNOWN_POSITION);
        ActorRef remoteMachine = OutputStationFactory.startStandaloneOutputStation(system, opcUaPort, machineId);
        ActorRef proxy = createParticipantProxy(system, machineId, opcUaPort);
        return new ParticipantInfo(machineId, position, opcUaPort, remoteMachine, proxy);
    }

    /**
     * Creates the opcua proxy for given machine or human
     *
     * @param system    actor system
     * @param machineId machine id
     * @param opcUaPort opcua port of remote machine/human
     * @return proxy
     */
    private static ActorRef createParticipantProxy(ActorSystem system, String machineId, int opcUaPort) {
        //These will be the opcua nodes to look for
        NodeId capabilityImplNode = NodeId.parse("ns=2;s=" + machineId + "/HANDSHAKE_FU/CAPABILITIES/CAPABILITY");
        NodeId resetMethod = NodeId.parse("ns=2;s=" + machineId + "/HANDSHAKE_FU/RESET");
        NodeId stopMethod = NodeId.parse("ns=2;s=" + machineId + "/HANDSHAKE_FU/STOP");
        NodeId stateVar = NodeId.parse("ns=2;s=" + machineId + "/HANDSHAKE_FU/STATE");
        //We get the capability
        AbstractCapability capability = IOStationCapability.getOutputStationCapability();

        try {
            //We create a client and connect to the machine. Then we use it in the opcua wrapper
            FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect(localhostOpcUaPrefix + opcUaPort);
            IOStationOPCUAWrapper opcuaWrapper;
            MachineEventBus machineEventBus = new MachineEventBus();
            opcuaWrapper = new IOStationOPCUAWrapper(machineEventBus, client, capabilityImplNode, stopMethod, resetMethod, stateVar, null);
            //Create the model
            Actor modelActor = createParticipantModelActor(opcUaPort);
            //Now we find the InterMachineEventBus and finally create the machine Proxy
            ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
            return system.actorOf(BasicIOStationActor.props(eventBusByRef, capability, modelActor, opcuaWrapper, machineEventBus),
                    modelActor.getActorName() + WRAPPER_POSTFIX);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates a model actor for given machine or human
     *
     * @param opcUaPort opcua port of remote machine/human
     * @return model actor
     */
    private static Actor createParticipantModelActor(int opcUaPort) {
        String actorId = "OutputStationActor";
        Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
        actor.setID(actorId);
        actor.setActorName(actorId);
        actor.setDisplayName(actorId);
        actor.setUri(localhostOpcUaPrefix + opcUaPort + "/OutputStationActor");
        return actor;
    }
}
