package fiab.iostation;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.iostation.opcua.OpcUaInputStationActor;
import fiab.iostation.opcua.OpcUaOutputStationActor;
import fiab.opcua.server.OPCUABase;

public class InputStationFactory {

    public static ActorRef startInputStation(ActorSystem system, MachineEventBus machineEventBus, int port, String machineName) {
        OPCUABase base = OPCUABase.createAndStartLocalServer(port, machineName);
        return system.actorOf(OpcUaInputStationActor.props(base, base.getRootNode(), machineEventBus), machineName);
    }

    public static ActorRef startStandaloneInputStation(ActorSystem system, int port, String machineName){
        OPCUABase base = OPCUABase.createAndStartLocalServer(port, machineName);
        return system.actorOf(OpcUaInputStationActor.props(base, base.getRootNode(), new MachineEventBus()), machineName);
    }

    public static ActorRef startStandaloneInputStation(ActorSystem system, OPCUABase opcuaBase){
        return system.actorOf(OpcUaInputStationActor.props(opcuaBase, opcuaBase.getRootNode(), new MachineEventBus()), opcuaBase.getMachineName());
    }
}
