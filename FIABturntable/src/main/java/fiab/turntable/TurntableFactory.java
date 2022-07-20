package fiab.turntable;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.opcua.server.OPCUABase;
import fiab.turntable.infrastructure.OpcUaMachineChildFUs;
import fiab.turntable.opcua.OpcUaTurntableActor;

public class TurntableFactory {

    public static ActorRef startTurntable(ActorSystem system, MachineEventBus machineEventBus, int port, String name) {
        OPCUABase base = OPCUABase.createAndStartLocalServer(port, name);
        return system.actorOf(OpcUaTurntableActor.props(base, base.getRootNode(), base.getMachineName(),
                machineEventBus, new IntraMachineEventBus(), new OpcUaMachineChildFUs(base)), base.getMachineName());
    }

    public static ActorRef startStandaloneTurntable(ActorSystem system, int port, String name) {
        return startTurntable(system, new MachineEventBus(), port, name);
    }

    public static ActorRef startStandaloneTurntable(ActorSystem system, OPCUABase opcuaBase){
        return system.actorOf(OpcUaTurntableActor.props(opcuaBase, opcuaBase.getRootNode(), opcuaBase.getMachineName(),
                new MachineEventBus(), new IntraMachineEventBus(), new OpcUaMachineChildFUs(opcuaBase)), opcuaBase.getMachineName());
    }
}
