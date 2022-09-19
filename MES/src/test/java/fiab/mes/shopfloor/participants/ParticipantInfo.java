package fiab.mes.shopfloor.participants;

import akka.actor.ActorRef;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import testutils.PortUtils;

import java.util.Optional;

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

    public ParticipantInfo(String machineId, Position position, int opcUaPort, ActorRef remoteMachine) {
        this.machineId = machineId;
        this.position = position;
        this.opcUaPort = opcUaPort;
        this.remoteMachine = remoteMachine;
        this.proxy = null;
    }

    /**
     * This stores the Position for a given machineId
     * A free port will be selected that can be used for starting up the virtual participant on localhost
     * @param machineId unique name of the machine
     * @param position position on the shopfloor
     */
    public ParticipantInfo(String machineId, Position position) {
        this.machineId = machineId;
        this.position = position;
        this.opcUaPort = PortUtils.findNextFreePort();  //This will automatically give us a free port we can use
        this.remoteMachine = null;
        this.proxy = null;
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

    public Optional<ActorRef> getRemoteMachine() {
        if(remoteMachine==null) return Optional.empty();
        return Optional.of(remoteMachine);
    }

    public Optional<ActorRef> getProxy() {
        if(proxy==null) return Optional.empty();
        return Optional.of(proxy);
    }

    public String getDiscoveryEndpoint() {
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
