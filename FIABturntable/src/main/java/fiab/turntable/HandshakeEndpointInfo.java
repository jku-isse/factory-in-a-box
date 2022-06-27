package fiab.turntable;

import akka.actor.ActorRef;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.handshake.actor.LocalEndpointStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HandshakeEndpointInfo {

    protected Map<String, LocalEndpointStatus> handshakeEPs = new HashMap<>();

    private final ActorRef self;

    protected HandshakeEndpointInfo(ActorRef self) {
        this.self = self;
    }

//		public HandshakeEndpointInfo(Map<String,LocalEndpointStatus> handshakeEPs) {
//			this.handshakeEPs = handshakeEPs;
//		}

    public Optional<LocalEndpointStatus> getHandshakeForCapId(String capabilityId) {
        if (capabilityId != null && handshakeEPs.containsKey(capabilityId))
            return Optional.ofNullable(handshakeEPs.get(capabilityId));
        else
            return Optional.empty();
    }

    public Optional<LocalEndpointStatus.LocalServerEndpointStatus> getServerHandshake(String capabilityId) {
        if (capabilityId != null && handshakeEPs.containsKey(capabilityId)) {
            LocalEndpointStatus les = handshakeEPs.get(capabilityId);
            if (les instanceof LocalEndpointStatus.LocalServerEndpointStatus) {
                return Optional.of(((LocalEndpointStatus.LocalServerEndpointStatus) les));
            }
        }
        return Optional.empty();
    }

    public Optional<LocalEndpointStatus.LocalClientEndpointStatus> getClientHandshake(String capabilityId) {
        if (capabilityId != null && handshakeEPs.containsKey(capabilityId)) {
            LocalEndpointStatus les = handshakeEPs.get(capabilityId);
            if (les instanceof LocalEndpointStatus.LocalClientEndpointStatus) {
                return Optional.of(((LocalEndpointStatus.LocalClientEndpointStatus) les));
            }
        }
        return Optional.empty();
    }

    public boolean isRegistered(String capabilityId) {
        return getHandshakeForCapId(capabilityId).isPresent();
    }

    public boolean isServerHandshake(String capabilityId){
        return getServerHandshake(capabilityId).isPresent();
    }

    public boolean isClientHandshake(String capabilityId){
        return getClientHandshake(capabilityId).isPresent();
    }

    public void addOrReplace(LocalEndpointStatus les) {
        handshakeEPs.put(les.getCapabilityId(), les);
    }

    public void updateServerHandshakeState(String capabilityId, ServerSideStates state){
        if(!isRegistered(capabilityId)) {
            addOrReplace(new LocalEndpointStatus.LocalServerEndpointStatus(capabilityId));
        }
        getServerHandshake(capabilityId).ifPresent(les -> les.setState(state));
    }

    public void updateClientHandshakeState(String capabilityId, ClientSideStates state){
        if(!isRegistered(capabilityId)){
            addOrReplace(new LocalEndpointStatus.LocalClientEndpointStatus(capabilityId));
        }
        getClientHandshake(capabilityId).ifPresent(les -> les.setState(state));
    }

    public boolean allHandshakesStopped(){
        long stoppedFus = handshakeEPs.values().stream()
                .filter(les -> les.getRawState().equals(ClientSideStates.STOPPED.toString())
                        || les.getRawState().equals(ServerSideStates.STOPPED.toString())).count();
        return stoppedFus == handshakeEPs.size();
    }
}
