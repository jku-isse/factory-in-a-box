package productioncell;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import fiab.core.capabilities.events.TimedEvent;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.opcua.CapabilityCentricActorSpawnerInterface;
import fiab.mes.opcua.CapabilityDiscoveryActor;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.opcua.CapabilityImplementationMetadata;
import productioncell.foldingstation.FoldingProductionCellCoordinator;

import java.util.*;

public class FoldingProductionCell {

    private static ActorSystem system;

    public static void main(String[] args) {
        system = ActorSystem.create("FoldingProductionCell");
        HardcodedDefaultTransportRoutingAndMapping routing = new HardcodedDefaultTransportRoutingAndMapping();
        TransportPositionLookup dns = new TransportPositionLookup();
        ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ActorRef transportCoord = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef foldingCellCoord = system.actorOf(FoldingProductionCellCoordinator.props(), FoldingProductionCellCoordinator.WELLKNOWN_LOOKUP_NAME);
        init();
    }

    private static void init(){
        Set<String> urlsToBrowse = getLocalhostLayout();

        Map<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface> capURI2Spawning = new HashMap<AbstractMap.SimpleEntry<String, CapabilityImplementationMetadata.ProvOrReq>, CapabilityCentricActorSpawnerInterface>();
        ShopfloorConfigurations.addDefaultSpawners(capURI2Spawning);
        //machineEventBus.tell(new SubscribeMessage(getRef(), new MESSubscriptionClassifier("Tester", "*")), getRef() );

        urlsToBrowse.forEach(url -> {
            ActorRef discovAct1 = system.actorOf(CapabilityDiscoveryActor.props());
            discovAct1.tell(new CapabilityDiscoveryActor.BrowseRequest(url, capURI2Spawning), ActorRef.noSender());
        });
    }

    public static Set<String> getLocalhostLayout() {
        Set<String> urlsToBrowse = new HashSet<String>();
        urlsToBrowse.add("opc.tcp://localhost:4840/milo"); //Pos34 input station (West of TT)
        urlsToBrowse.add("opc.tcp://localhost:4842/milo"); // TT1 Pos20
        urlsToBrowse.add("opc.tcp://localhost:4845/milo"); // Pos31 FoldingStation1 (North of TT)
        urlsToBrowse.add("opc.tcp://localhost:4847/milo"); // Pos37 FoldingStation2 (South of TT)
        urlsToBrowse.add("opc.tcp://localhost:4850/milo"); // Pos23 OutputStation (East of TT)
        return urlsToBrowse;
    }

}
