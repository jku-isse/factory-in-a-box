package fiab.mes.proxy.testutil;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.proxy.ioStation.inputStation.InputStationDiscoveryTest;
import fiab.mes.transport.actor.transportmodule.InternalCapabilityToPositionMapping;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionParser;
import fiab.opcua.CapabilityImplementationMetadata;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class DiscoveryUtil {

    private final ActorSystem system;
    private final ActorRef testActor;
    private final Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning;
    private final InternalCapabilityToPositionMapping capabilityToPositionMapping;

    public DiscoveryUtil(ActorSystem system, ActorRef testActor, TransportPositionParser positionParser){
        this.system = system;
        this.testActor = testActor;
        this.capURI2Spawning = new HashMap<>();
        this.capabilityToPositionMapping = new HardcodedDefaultTransportRoutingAndMapping();
        ShopfloorConfigurations.addSpawners(capURI2Spawning, positionParser);
    }

    public DiscoveryUtil(ActorSystem system, ActorRef testActor, TransportPositionParser positionParser, InternalCapabilityToPositionMapping capabilityToPositionMapping){
        this.system = system;
        this.testActor = testActor;
        this.capURI2Spawning = new HashMap<>();
        this.capabilityToPositionMapping = capabilityToPositionMapping;
        ShopfloorConfigurations.addSpawners(capURI2Spawning, positionParser, capabilityToPositionMapping);
    }

    public void discoverCapabilityForEndpoint(String endpointUrl){
            ActorRef discoveryActor = system.actorOf(CapabilityDiscoveryActor.props());
            discoveryActor.tell(new CapabilityDiscoveryActor.BrowseRequest(endpointUrl, capURI2Spawning), testActor);
    }
}
