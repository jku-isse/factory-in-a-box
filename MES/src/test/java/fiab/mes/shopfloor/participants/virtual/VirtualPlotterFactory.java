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
}
