package fiab.mes.shopfloor.participants.virtual;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import fiab.core.capabilities.transport.TransportModuleCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.core.capabilities.wiring.WiringInfoBuilder;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.client.messages.WiringRequest;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.shopfloor.participants.PositionMap;
import fiab.mes.shopfloor.utils.ShopfloorUtils;
import fiab.mes.shopfloor.utils.TransportRoutingAndMappingInterface;
import fiab.mes.shopfloor.utils.TurntableCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportmodule.wrapper.TransportModuleOPCUAWrapper;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.turntable.TurntableFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.List;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.*;
import static fiab.mes.shopfloor.participants.ParticipantInfo.WRAPPER_POSTFIX;
import static fiab.mes.shopfloor.participants.ParticipantInfo.localhostOpcUaPrefix;
import static fiab.mes.shopfloor.utils.ShopfloorUtils.*;

public class VirtualTurntableFactory {

    public static List<String> serverHandshakeCapabilities = List.of(
            TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SELF,    //We include ourselves as well
            TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_NORTH_SERVER,
            TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_EAST_SERVER,
            TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SOUTH_SERVER,
            TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_WEST_SERVER);

    /**
     * Starts a new turntable instance at an arbitrary free opcua port and a given position on the shopfloor
     * Then a proxy for machine is spawned that can be used to communicate events
     *
     * @param system      The actor system
     * @param machineId   unique machine name/identifier
     * @param positionMap map containing names mapped to positions
     * @return machine info
     */
    public static ParticipantInfo initTurntableAndReturnInfo(ActorSystem system, String machineId,
                                                             TurntableCapabilityToPositionMapping capToPositionMapping,
                                                             PositionMap positionMap,
                                                             TransportRoutingAndMappingInterface routingAndMapping) {
        int opcUaPort = positionMap.get(machineId).getOpcUaPort();
        Position position = positionMap.getPositionForId(machineId);
        ActorRef remoteMachine = TurntableFactory.startStandaloneTurntable(system, opcUaPort, machineId);
        wireTurntableToOtherMachines(remoteMachine, position, capToPositionMapping, positionMap, routingAndMapping);
        return new ParticipantInfo(machineId, position, opcUaPort, remoteMachine);
    }

    /**
     * We can use the information provided in routing and mapping combined with the position and capability mapping
     * to automatically wire the turntable to the other participants
     */
    private static void wireTurntableToOtherMachines(ActorRef remoteMachine,
                                                     Position selfPos,
                                                     TurntableCapabilityToPositionMapping capToPositionMapping,
                                                     PositionMap positionMap,
                                                     TransportRoutingAndMappingInterface routingAndMapping) {
        try {
            WiringInfo wiringInfo;
            for (Position targetPos : capToPositionMapping.getAllMappedPositions()) {
                if (targetPos.equals(selfPos)) continue;  //We don't need to wire ourselves
                wiringInfo = createWiringInfoForActorAtPosition(targetPos, selfPos, positionMap, routingAndMapping);
                if (serverHandshakeCapabilities.contains(wiringInfo.getLocalCapabilityId()))
                    continue;   //We skip server nodes
                remoteMachine.tell(new WiringRequest("VirtualTTFactory", wiringInfo), ActorRef.noSender());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static WiringInfo createWiringInfoForActorAtPosition(Position targetPos, Position selfPos, PositionMap positionMap, TransportRoutingAndMappingInterface routing) throws Exception {
        String localCapId = routing.getCapabilityIdForPosition(targetPos, selfPos)
                .orElseThrow(() -> new Exception("Could not find cap id for target position " + targetPos + " in routing"));
        String remoteMachineId = positionMap.getMachineIdForPosition(routing.getPositionForCapability(localCapId, selfPos))
                .orElseThrow(() -> new Exception("Could not find position for capability " + localCapId + " in routing"));

        String remoteCapId = "DefaultHandshakeServerSide";
        String remoteEndpointURL = localhostOpcUaPrefix + positionMap.getOpcUaPortForId(remoteMachineId);
        String remoteNodeId = "ns=2;s=" + remoteMachineId + "/HANDSHAKE_FU/CAPABILITIES/CAPABILITY";
        String remoteRole = "RemoteRole1";

        //In case the remote server is an IO station, we need to adjust the remote nodeId to omit /HANDSHAKE_FU/
        if (remoteMachineId.equals(INPUT_STATION) || remoteMachineId.equals(OUTPUT_STATION)) {
            remoteNodeId = "ns=2;s=" + remoteMachineId + "/CAPABILITIES/CAPABILITY";
        }
        //Turntables use a non-default remote cap id. We assume all machines face the same direction
        //Under this assumption, we just take the opposite side of the local hs cap id
        if (remoteMachineId.equals(TURNTABLE_1) || remoteMachineId.equals(TURNTABLE_2)) {
            String adjacentCapability = getAdjacentTurntableHandshakeCapability(localCapId);
            remoteCapId = adjacentCapability;
            remoteNodeId = "ns=2;s=" + remoteMachineId + "/HANDSHAKE_FU_" + adjacentCapability + "/CAPABILITIES/CAPABILITY";
        }

        return WiringInfoBuilder.create()
                .setLocalCapabilityId(localCapId)
                .setRemoteCapabilityId(remoteCapId)
                .setRemoteEndpointURL(remoteEndpointURL)
                .setRemoteNodeId(remoteNodeId)
                .setRemoteRole(remoteRole)
                .build();
    }

    private static String getAdjacentTurntableHandshakeCapability(String localCapId) {
        String remoteCapId = "DefaultHandshakeServerSide";
        switch (localCapId) {
            case TRANSPORT_MODULE_NORTH_CLIENT:
                remoteCapId = TRANSPORT_MODULE_SOUTH_SERVER;
                break;
            case TRANSPORT_MODULE_EAST_CLIENT:
                remoteCapId = TRANSPORT_MODULE_WEST_SERVER;
                break;
            case TRANSPORT_MODULE_SOUTH_CLIENT:
                remoteCapId = TRANSPORT_MODULE_NORTH_SERVER;
                break;
            case TRANSPORT_MODULE_WEST_CLIENT:
                remoteCapId = TRANSPORT_MODULE_EAST_SERVER;
                break;
        }
        return remoteCapId;
    }
}
