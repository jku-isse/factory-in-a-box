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
import fiab.mes.shopfloor.participants.PositionMap;
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
     * @param system      The actor system
     * @param positionMap mapping of unique machine name/id to position
     * @return machine info
     */
    public static ParticipantInfo initOutputStationAndReturnInfo(ActorSystem system, String machineId, PositionMap positionMap) {
        int opcUaPort = positionMap.get(machineId).getOpcUaPort();
        Position position = positionMap.getPositionForId(machineId);
        ActorRef remoteMachine = OutputStationFactory.startStandaloneOutputStation(system, opcUaPort, machineId);
        return new ParticipantInfo(machineId, position, opcUaPort, remoteMachine);
    }
}
