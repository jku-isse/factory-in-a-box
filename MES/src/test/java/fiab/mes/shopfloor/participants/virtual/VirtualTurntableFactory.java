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

import static fiab.mes.shopfloor.participants.ParticipantInfo.WRAPPER_POSTFIX;
import static fiab.mes.shopfloor.participants.ParticipantInfo.localhostOpcUaPrefix;

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
     * Starts a new turntable instance at an arbitrary free opcua port and a given position on the shopfloor
     * Then a proxy for machine is spawned that can be used to communicate events
     *
     * @param system            The actor system
     * @param machineId         unique machine name/identifier
     * @param positionMap       map containing names mapped to positions
     * @param positionLookup    position lookup
     * @param routingAndMapping routing
     * @return machine info with proxy
     */
    public static ParticipantInfo initTurntableWithProxyAndReturnInfo(ActorSystem system, String machineId,
                                                                      TurntableCapabilityToPositionMapping capToPositionMapping,
                                                                      PositionMap positionMap,
                                                                      TransportPositionLookupInterface positionLookup,
                                                                      TransportRoutingAndMappingInterface routingAndMapping) {
        int opcUaPort = positionMap.get(machineId).getOpcUaPort();
        Position position = positionMap.getPositionForId(machineId);
        ActorRef remoteMachine = TurntableFactory.startStandaloneTurntable(system, opcUaPort, machineId);
        wireTurntableToOtherMachines(remoteMachine, position, capToPositionMapping, positionMap, routingAndMapping);
        ActorRef proxy = createParticipantProxy(system, machineId, position, opcUaPort, positionLookup, routingAndMapping);
        return new ParticipantInfo(machineId, position, opcUaPort, remoteMachine, proxy);
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
                if(serverHandshakeCapabilities.contains(wiringInfo.getLocalCapabilityId())) continue;   //We skip server nodes
                remoteMachine.tell(new WiringRequest("VirtualTTFactory", wiringInfo), ActorRef.noSender());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the opcua proxy for given machine or human
     *
     * @param system    actor system
     * @param machineId machine id
     * @param position  position on the shopfloor as String
     * @param opcUaPort opcua port of remote machine/human
     * @return proxy
     */
    private static ActorRef createParticipantProxy(ActorSystem system, String machineId, Position position, int opcUaPort,
                                                   TransportPositionLookupInterface positionLookup,
                                                   TransportRoutingAndMappingInterface routingAndMapping) {
        //These will be the opcua nodes to look for
        NodeId capabilityImplNode = NodeId.parse("ns=2;s=" + machineId + "/CAPABILITIES/CAPABILITY");
        NodeId resetMethod = NodeId.parse("ns=2;s=" + machineId + "/Reset");
        NodeId stopMethod = NodeId.parse("ns=2;s=" + machineId + "/Stop");
        NodeId transportReq = NodeId.parse("ns=2;s=" + machineId + "/TransportRequest");
        NodeId stateVar = NodeId.parse("ns=2;s=" + machineId + "/STATE");
        //We get the capability
        AbstractCapability capability = TransportModuleCapability.getTransportCapability();

        try {
            //We create a client and connect to the machine. Then we use it in the opcua wrapper
            FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect(localhostOpcUaPrefix + opcUaPort);
            TransportModuleOPCUAWrapper opcuaWrapper;
            MachineEventBus machineEventBus = new MachineEventBus();
            opcuaWrapper = new TransportModuleOPCUAWrapper(machineEventBus, client, capabilityImplNode, stopMethod, resetMethod, stateVar, transportReq, null);
            //Create the model
            Actor modelActor = createParticipantModelActor(machineId, opcUaPort);
            //Now we find the InterMachineEventBus and finally create the machine Proxy
            ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
            return system.actorOf(BasicTransportModuleActor.props(eventBusByRef, capability, modelActor, opcuaWrapper, position, machineEventBus, positionLookup, routingAndMapping),
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
    private static Actor createParticipantModelActor(String machineName, int opcUaPort) {
        Actor actor = ActorCoreModel.ActorCoreModelFactory.eINSTANCE.createActor();
        actor.setID(machineName);
        actor.setActorName(machineName);
        actor.setDisplayName(machineName);
        actor.setUri(localhostOpcUaPrefix + opcUaPort + "/" + machineName);
        return actor;
    }

    private static WiringInfo createWiringInfoForActorAtPosition(Position targetPos, Position selfPos, PositionMap positionMap, TransportRoutingAndMappingInterface routing) throws Exception {
        String localCapId = routing.getCapabilityIdForPosition(targetPos, selfPos)
                .orElseThrow(() -> new Exception("Could not find cap id for target position " + targetPos + " in routing"));
        String remoteMachineId = positionMap.getMachineIdForPosition(routing.getPositionForCapability(localCapId, selfPos))
                .orElseThrow(() -> new Exception("Could not find position for capability " + localCapId + " in routing"));

        String remoteCapId = "DefaultHandshakeServerSide";
        String remoteEndpointURL = localhostOpcUaPrefix + positionMap.getOpcUaPortForId(remoteMachineId);
        String remoteNodeId = "ns=2;" + remoteMachineId + "HANDSHAKE_FU/CAPABILITIES/CAPABILITY";
        String remoteRole = "RemoteRole1";

        return WiringInfoBuilder.create()
                .setLocalCapabilityId(localCapId)
                .setRemoteCapabilityId(remoteCapId)
                .setRemoteEndpointURL(remoteEndpointURL)
                .setRemoteNodeId(remoteNodeId)
                .setRemoteRole(remoteRole)
                .build();
    }
}
