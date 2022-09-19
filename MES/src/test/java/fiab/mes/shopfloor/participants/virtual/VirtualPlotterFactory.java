package fiab.mes.shopfloor.participants.virtual;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.iostation.OutputStationFactory;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.actor.plotter.BasicMachineActor;
import fiab.mes.machine.actor.plotter.wrapper.PlotterOPCUAWrapper;
import fiab.mes.shopfloor.participants.ParticipantInfo;
import fiab.mes.shopfloor.participants.PositionMap;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface;
import fiab.opcua.client.FiabOpcUaClient;
import fiab.opcua.client.OPCUAClientFactory;
import fiab.plotter.PlotterFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import testutils.PortUtils;

import java.util.Map;

import static fiab.mes.shopfloor.participants.ParticipantInfo.WRAPPER_POSTFIX;
import static fiab.mes.shopfloor.participants.ParticipantInfo.localhostOpcUaPrefix;

public class VirtualPlotterFactory {

    /**
     * Starts a new outputStation instance at an arbitrary free opcua port and a given position on the shopfloor
     * Then a proxy for machine is spawned that can be used to communicate events
     *
     * @param system      The actor system
     * @param positionMap mapping of unique machine name/id to position
     * @return machine info
     */
    public static ParticipantInfo initPlotterAndReturnInfo(ActorSystem system, String machineId,
                                                           WellknownPlotterCapability.SupportedColors color,
                                                           PositionMap positionMap) {
        int opcUaPort = positionMap.get(machineId).getOpcUaPort();
        TransportRoutingInterface.Position position = positionMap.getPositionForId(machineId);
        ActorRef remoteMachine = PlotterFactory.startStandalonePlotter(system, opcUaPort, machineId, color);
        return new ParticipantInfo(machineId, position, opcUaPort, remoteMachine);
    }

    /**
     * Starts a new outputStation instance at an arbitrary free opcua port and a given position on the shopfloor
     * Then a proxy for machine is spawned that can be used to communicate events
     *
     * @param system      The actor system
     * @param positionMap mapping of unique machine name/id to position
     * @return machine info with proxy
     */
    public static ParticipantInfo initPlotterWithProxyAndReturnInfo(ActorSystem system, String machineId,
                                                                    WellknownPlotterCapability.SupportedColors color,
                                                                    PositionMap positionMap) {
        int opcUaPort = positionMap.get(machineId).getOpcUaPort();
        TransportRoutingInterface.Position position = positionMap.getPositionForId(machineId);
        ActorRef remoteMachine = PlotterFactory.startStandalonePlotter(system, opcUaPort, machineId, color);
        ActorRef proxy = createParticipantProxy(system, machineId, color, opcUaPort);
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
    private static ActorRef createParticipantProxy(ActorSystem system, String machineId,
                                                   WellknownPlotterCapability.SupportedColors color, int opcUaPort) {
        //These will be the opcua nodes to look for
        NodeId capabilityImplNode = NodeId.parse("ns=2;s=" + machineId + "/CAPABILITIES/CAPABILITY");
        NodeId resetMethod = NodeId.parse("ns=2;s=" + machineId + "/RESET");
        NodeId stopMethod = NodeId.parse("ns=2;s=" + machineId + "/STOP");
        NodeId stateVar = NodeId.parse("ns=2;s=" + machineId + "/STATE");
        NodeId plotMethod = NodeId.parse("ns=2;s=" + machineId + "/PLOT");
        //NodeId initHandshakeNode = NodeId.parse("ns=2;s=" + machineId + "/HANDSHAKE_FU/INIT_HANDOVER");
        //NodeId startHandshakeNode = NodeId.parse("ns=2;s=" + machineId + "/HANDSHAKE_FU/START_HANDOVER");
        //We get the capability
        AbstractCapability capability = WellknownPlotterCapability.getColorPlottingCapability(color);

        try {
            //We create a client and connect to the machine. Then we use it in the opcua wrapper
            FiabOpcUaClient client = OPCUAClientFactory.createFIABClientAndConnect(localhostOpcUaPrefix + opcUaPort);
            PlotterOPCUAWrapper opcuaWrapper;
            MachineEventBus machineEventBus = new MachineEventBus();
            opcuaWrapper = new PlotterOPCUAWrapper(machineEventBus, client, capabilityImplNode, stopMethod, resetMethod, stateVar, plotMethod, null);
            //Create the model
            Actor modelActor = createParticipantModelActor(machineId, opcUaPort);
            //Now we find the InterMachineEventBus and finally create the machine Proxy
            ActorSelection eventBusByRef = system.actorSelection("/user/" + InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
            return system.actorOf(BasicMachineActor.props(eventBusByRef, capability, modelActor, opcuaWrapper, machineEventBus),
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
