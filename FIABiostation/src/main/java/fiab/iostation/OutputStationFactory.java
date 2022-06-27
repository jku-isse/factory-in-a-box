package fiab.iostation;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.iostation.opcua.OpcUaOutputStationActor;
import fiab.opcua.server.OPCUABase;

public class OutputStationFactory {

    public static ActorRef startStandaloneOutputStation(ActorSystem system, MachineEventBus machineEventBus, int port, String machineName) {
        OPCUABase base = OPCUABase.createAndStartLocalServer(port, machineName);
        return system.actorOf(OpcUaOutputStationActor.props(base, base.getRootNode(), machineEventBus), machineName);
    }

    public static ActorRef startStandaloneOutputStation(ActorSystem system, int port, String machineName) {
        OPCUABase base = OPCUABase.createAndStartLocalServer(port, machineName);
        return system.actorOf(OpcUaOutputStationActor.props(base, base.getRootNode(), new MachineEventBus()), machineName);
    }

    public static ActorRef startStandaloneOutputStation(ActorSystem system, OPCUABase opcuaBase){
        return system.actorOf(OpcUaOutputStationActor.props(opcuaBase, opcuaBase.getRootNode(), new MachineEventBus()), opcuaBase.getMachineName());
    }
}
