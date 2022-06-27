package fiab.mes.proxy.testutil;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.proxy.ioStation.inputStation.InputStationDiscoveryTest;
import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.opcua.CapabilityImplementationMetadata;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class DiscoveryUtil {

    private final ActorSystem system;
    private final ActorRef testActor;
    private final ActorRef machineEventBusWrapper;
    private final TransportPositionParser positionParser;
    private final Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning;

    public DiscoveryUtil(ActorSystem system, ActorRef testActor, ActorRef machineEventBusWrapper, TransportPositionParser positionParser){
        this.system = system;
        this.testActor = testActor;
        this.machineEventBusWrapper = machineEventBusWrapper;
        this.positionParser = positionParser;
        this.capURI2Spawning = new HashMap<>();

        ShopfloorConfigurations.addSpawners(capURI2Spawning, positionParser);
    }

    public void discoverCapabilityForEndpoint(String endpointUrl){
            ActorRef discoveryActor = system.actorOf(CapabilityDiscoveryActor.props());
            discoveryActor.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointUrl, capURI2Spawning), testActor);
    }
}
