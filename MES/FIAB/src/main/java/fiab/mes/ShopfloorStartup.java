package fiab.mes;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.HttpsConnectionContext;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import fiab.mes.auth.HttpsConfigurator;
import fiab.mes.machine.MachineEntryActor;
import fiab.mes.order.actor.OrderEntryActor;
import fiab.mes.restendpoint.ActorRestEndpoint;

import java.util.concurrent.CompletionStage;

public class ShopfloorStartup extends AllDirectives {

  
  public static void main(String[] args) throws Exception {

	  String jsonDiscoveryFile = System.getProperty("jsondiscoveryfile");
	  ActorSystem system = ActorSystem.create("routes");

	  CompletionStage<ServerBinding> binding = startup(jsonDiscoveryFile, system);

	  System.out.println("Server online at https://localhost:8080/\nPress RETURN to stop...");
	  System.in.read(); // let it run until user presses return

	  binding
	  .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
	  .thenAccept(unbound -> system.terminate()); // and shutdown when done
  }
  
  public static CompletionStage<ServerBinding> startup(String jsonDiscoveryFile, ActorSystem system) {
	    // boot up server using the route as defined below	    	      
	    final Http http = Http.get(system);
	    
	    HttpsConnectionContext https = HttpsConfigurator.useHttps(system);
	    http.setDefaultServerHttpContext(https);
	    
	    final ActorMaterializer materializer = ActorMaterializer.create(system);

	    DefaultShopfloorInfrastructure shopfloor = new DefaultShopfloorInfrastructure(system);
	    ActorRef orderEntryActor = system.actorOf(OrderEntryActor.props(), OrderEntryActor.WELLKNOWN_LOOKUP_NAME);
	    ActorRef machineEntryActor = system.actorOf(MachineEntryActor.props(), MachineEntryActor.WELLKNOWN_LOOKUP_NAME);
	    	    
	    if (jsonDiscoveryFile != null)
	    	new ShopfloorConfigurations.JsonFilePersistedDiscovery(jsonDiscoveryFile).triggerDiscoveryMechanism(system);
	    else {
	    //new ShopfloorConfigurations.VirtualInputOutputTurntableOnly().triggerDiscoveryMechanism(system);
	    	new ShopfloorConfigurations.NoDiscovery().triggerDiscoveryMechanism(system);
	    }
	    ActorRestEndpoint app = new ActorRestEndpoint(system, orderEntryActor, machineEntryActor);

	    final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
	    final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
	        ConnectHttp.toHost("0.0.0.0", 8080), materializer);	    
	    return binding;
  }
  
  
}