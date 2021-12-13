package fiab.mes.productioncell;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.HttpsConnectionContext;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import fiab.mes.DefaultShopfloorInfrastructure;
import fiab.mes.ShopfloorConfigurations;
import fiab.mes.auth.HttpsConfigurator;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.machine.MachineEntryActor;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.productioncell.foldingstation.DefaultFoldingCellTransportPositionLookup;
import fiab.mes.productioncell.foldingstation.HardcodedFoldingCellTransportRoutingAndMapping;
import fiab.mes.restendpoint.ActorRestEndpoint;
import fiab.mes.transport.actor.transportsystem.DefaultTransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.HardcodedDefaultTransportRoutingAndMapping;
import fiab.mes.transport.actor.transportsystem.TransportSystemCoordinatorActor;
import fiab.mes.productioncell.foldingstation.FoldingProductionCellCoordinator;

import java.util.concurrent.CompletionStage;

public class FoldingProductionCell {

    private static String production_cell_name;
    private static ActorSystem system;

    public static void main(String[] args) {
        if (args.length > 0) {
            production_cell_name = args[0];
        } else {
            production_cell_name = "LocalFoldingProductionCell";
        }
        system = ActorSystem.create(production_cell_name);
        HardcodedFoldingCellTransportRoutingAndMapping routing = new HardcodedFoldingCellTransportRoutingAndMapping();
        DefaultFoldingCellTransportPositionLookup dns = new DefaultFoldingCellTransportPositionLookup();
        ActorRef machineEventBus = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
        ActorRef transportCoord = system.actorOf(TransportSystemCoordinatorActor.props(routing, dns, 1), TransportSystemCoordinatorActor.WELLKNOWN_LOOKUP_NAME);
        ActorRef foldingCellCoord = system.actorOf(FoldingProductionCellCoordinator.props(), FoldingProductionCellCoordinator.WELLKNOWN_LOOKUP_NAME);
        loadProductionCellLayoutFromFile();
    }

    public static void loadProductionCellLayoutFromFile() {
        ShopfloorConfigurations.JsonFilePersistedDiscovery discovery = new ShopfloorConfigurations.JsonFilePersistedDiscovery(production_cell_name);
        HardcodedFoldingCellTransportRoutingAndMapping routing = new HardcodedFoldingCellTransportRoutingAndMapping();
        DefaultFoldingCellTransportPositionLookup dns = new DefaultFoldingCellTransportPositionLookup();
        discovery.triggerDiscoveryMechanism(system, dns, routing);
    }

    public static CompletionStage<ServerBinding> startup(String jsonDiscoveryFile, int expectedTTs, ActorSystem system) {
        // boot up server using the route as defined below
        final Http http = Http.get(system);

        HttpsConnectionContext https = HttpsConfigurator.useHttps(system);
        http.setDefaultServerHttpContext(https);

        final ActorMaterializer materializer = ActorMaterializer.create(system);

        DefaultProductionCellInfrastructure shopfloor = new DefaultProductionCellInfrastructure(system, expectedTTs);
        ActorRef orderEntryActor = system.actorOf(OrderEntryActor.props(), "Folding" + OrderEntryActor.WELLKNOWN_LOOKUP_NAME);

        ActorRef machineEntryActor = system.actorOf(MachineEntryActor.props(), "Folding" + MachineEntryActor.WELLKNOWN_LOOKUP_NAME);

        if (jsonDiscoveryFile != null) {
            HardcodedFoldingCellTransportRoutingAndMapping routing = new HardcodedFoldingCellTransportRoutingAndMapping();
            DefaultFoldingCellTransportPositionLookup dns = new DefaultFoldingCellTransportPositionLookup();
            new ShopfloorConfigurations.JsonFilePersistedDiscovery(jsonDiscoveryFile).triggerDiscoveryMechanism(system, dns, routing);
        }else {
            //new ShopfloorConfigurations.VirtualInputOutputTurntableOnly().triggerDiscoveryMechanism(system);
            new ShopfloorConfigurations.NoDiscovery().triggerDiscoveryMechanism(system);
        }
        ActorRestEndpoint app = new ActorRestEndpoint(system, orderEntryActor, machineEntryActor);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("0.0.0.0", 8081), materializer);
        return binding;
    }

}
