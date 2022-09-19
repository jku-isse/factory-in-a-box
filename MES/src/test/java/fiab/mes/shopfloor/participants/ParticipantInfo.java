package fiab.mes.shopfloor.participants;

import akka.actor.ActorRef;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;

public class ParticipantInfo {

    public static String WRAPPER_POSTFIX = "Wrapper";
    public static String localhostOpcUaPrefix = "opc.tcp://127.0.0.1:";

    private final String machineId;
    private final Position position;
    private final int opcUaPort;
    private final ActorRef remoteMachine;
    private final ActorRef proxy;

    public ParticipantInfo(String machineId, Position position, int opcUaPort, ActorRef remoteMachine, ActorRef proxy) {
        this.machineId = machineId;
        this.position = position;
        this.opcUaPort = opcUaPort;
        this.remoteMachine = remoteMachine;
        this.proxy = proxy;
    }

    public String getMachineId() {
        return machineId;
    }

    public Position getPosition() {
        return position;
    }

    public int getOpcUaPort() {
        return opcUaPort;
    }

    public ActorRef getRemoteMachine() {
        return remoteMachine;
    }

    public ActorRef getProxy() {
        return proxy;
    }

    public String getDiscoveryEndpoint(){
        return localhostOpcUaPrefix + opcUaPort;
    }

    @Override
    public String toString() {
        return "ParticipantInfo{" +
                "machineId='" + machineId + '\'' +
                ", position=" + position +
                ", opcUaPort=" + opcUaPort +
                ", remoteMachine=" + remoteMachine +
                ", proxy=" + proxy +
                '}';
    }
}
