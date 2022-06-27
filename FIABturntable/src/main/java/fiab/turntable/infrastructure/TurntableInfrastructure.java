package fiab.turntable.infrastructure;

import akka.actor.ActorContext;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.IntraMachineEventBus;

import java.util.HashMap;
import java.util.Map;

public abstract class TurntableInfrastructure {

    protected final Map<String, FUConnector> fuConnectors;

    public TurntableInfrastructure(){
        this.fuConnectors = new HashMap<>();
    }

    public Map<String, FUConnector> getFuConnectors() {
        return fuConnectors;
    }

    public FUConnector getFUConnectorForCapabilityId(String capabilityId) {
        return fuConnectors.get(capabilityId);
    }

    public abstract void setupInfrastructure(ActorContext context, IntraMachineEventBus intraMachineEventBus);
}
