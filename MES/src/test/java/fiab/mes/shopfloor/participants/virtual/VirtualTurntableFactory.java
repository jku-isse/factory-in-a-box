package fiab.mes.shopfloor.participants.virtual;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.iostation.OutputStationFactory;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.iostation.BasicIOStationActor;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.shopfloor.utils.TransportRoutingAndMappingInterface;
import fiab.mes.transport.actor.transportmodule.BasicTransportModuleActor;
import fiab.mes.transport.actor.transportmodule.wrapper.TransportModuleOPCUAWrapper;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookupInterface;
import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import testutils.PortUtils;

import java.util.Map;

import static fiab.core.capabilities.transport.TurntableModuleWellknownCapabilityIdentifiers.TRANSPORT_MODULE_SELF;
import static fiab.mes.shopfloor.participants.ParticipantInfo.WRAPPER_POSTFIX;
import static fiab.mes.shopfloor.participants.ParticipantInfo.localhostOpcUaPrefix;

public class VirtualTurntableFactory {

    /**
     * Starts a new turntable instance at an arbitrary free opcua port and a given position on the shopfloor
     * Then a proxy for machine is spawned that can be used to communicate events
     * @param system The actor system
     * @param machineId unique machine name/identifier
     * @param positionMap map containing names mapped to positions
     * @param positionLookup position lookup
     * @param routingAndMapping routing
     * @return proxy as actorRef for virtual machine
     */
    public static ParticipantInfo initTurntableAndReturnInfo(ActorSystem system, String machineId,
                                                             Map<String, Position> positionMap,
                                                             TransportPositionLookupInterface positionLookup,
                                                             TransportRoutingAndMappingInterface routingAndMapping) {
        int opcUaPort = PortUtils.findNextFreePort();
        Position position = positionMap.getOrDefault(machineId, TransportRoutingInterface.UNKNOWN_POSITION);
        ActorRef remoteMachine = OutputStationFactory.startStandaloneOutputStation(system, opcUaPort, machineId);
        ActorRef proxy = createParticipantProxy(system, machineId, position, opcUaPort, positionLookup, routingAndMapping);
        return new ParticipantInfo(machineId, position, opcUaPort, remoteMachine, proxy);
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
        AbstractCapability capability = IOStationCapability.getOutputStationCapability();

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
}
