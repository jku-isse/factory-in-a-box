package fiab.iostation;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import client.ClientNode;
import client.ROSClient;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.iostation.opcua.OpcUaInputStationActor;
import fiab.iostation.opcua.OpcUaInputStationActorROS;
import fiab.iostation.opcua.OpcUaOutputStationActor;
import fiab.opcua.server.OPCUABase;
import internal.FIABNodeConfig;
import org.ros.exception.ServiceNotFoundException;
import org.ros.node.NodeConfiguration;
import ros_basic_machine_msg.ResetService;
import ros_basic_machine_msg.StopService;
import ros_io_msg.EjectService;

import java.net.URI;
import java.net.URISyntaxException;

public class InputStationFactory {

    public static ActorRef startInputStation(ActorSystem system, MachineEventBus machineEventBus, int port, String machineName) {
        OPCUABase base = OPCUABase.createAndStartLocalServer(port, machineName);
        return system.actorOf(OpcUaInputStationActor.props(base, base.getRootNode(), machineEventBus), machineName);
    }

    public static ActorRef startStandaloneInputStation(ActorSystem system, int port, String machineName) {
        OPCUABase base = OPCUABase.createAndStartLocalServer(port, machineName);
        return system.actorOf(OpcUaInputStationActor.props(base, base.getRootNode(), new MachineEventBus()), machineName);
    }

    public static ActorRef startStandaloneInputStation(ActorSystem system, OPCUABase opcuaBase) {
        return system.actorOf(OpcUaInputStationActor.props(opcuaBase, opcuaBase.getRootNode(), new MachineEventBus()), opcuaBase.getMachineName());
    }

    public static ActorRef startStandaloneInputStationROS(ActorSystem system, OPCUABase opcuaBase, ROSClient client) {
        return system.actorOf(OpcUaInputStationActorROS.props(client, opcuaBase, opcuaBase.getRootNode(),
                new MachineEventBus()), opcuaBase.getMachineName());
    }

    public static ActorRef startStandaloneInputStationROS(ActorSystem system, int port, String machineName, String rosMasterIp) {
        OPCUABase opcuaBase = OPCUABase.createAndStartLocalServer(port, machineName);
        return startStandaloneInputStationROS(system, opcuaBase, createROSClientForInputStation(opcuaBase.getMachineName(), rosMasterIp));
    }

    public static ROSClient createROSClientForInputStation(String nodeName, String rosMasterIp) {
        try {
            NodeConfiguration nodeConfiguration = FIABNodeConfig.createNodeConfiguration("127.0.0.1",
                    nodeName, new URI(rosMasterIp));
            ROSClient rosClient = ROSClient.newInstance(ClientNode.class, nodeConfiguration);
            rosClient.createServiceClient("FIAB_reset_service", ResetService._TYPE);
            rosClient.createServiceClient("FIAB_eject_service", EjectService._TYPE);
            rosClient.createServiceClient("FIAB_stop_service", StopService._TYPE);
            return rosClient;
        } catch (ServiceNotFoundException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
}
