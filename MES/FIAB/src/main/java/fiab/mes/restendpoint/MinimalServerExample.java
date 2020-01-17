package fiab.mes.restendpoint;

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
import fiab.mes.DefaultShopfloorInfrastructure;
import fiab.mes.auth.HttpsConfigurator;
import fiab.mes.machine.MachineEntryActor;
import fiab.mes.order.actor.OrderEntryActor;
import java.util.concurrent.CompletionStage;

public class MinimalServerExample extends AllDirectives {

  
  public static void main(String[] args) throws Exception {
    // boot up server using the route as defined below
    ActorSystem system = ActorSystem.create("routes");
      
    final Http http = Http.get(system);
    
    HttpsConnectionContext https = HttpsConfigurator.useHttps(system);
    http.setDefaultServerHttpContext(https);
    
    final ActorMaterializer materializer = ActorMaterializer.create(system);

    DefaultShopfloorInfrastructure shopfloor = new DefaultShopfloorInfrastructure(system);
    ActorRef orderEntryActor = system.actorOf(OrderEntryActor.props());
    ActorRef machineEntryActor = system.actorOf(MachineEntryActor.props());
    ActorRestEndpoint app = new ActorRestEndpoint(system, orderEntryActor, machineEntryActor);

    final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
    final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
        ConnectHttp.toHost("localhost", 8080), materializer);

    System.out.println("Server online at https://localhost:8080/\nPress RETURN to stop...");
    System.in.read(); // let it run until user presses return

    binding
        .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
        .thenAccept(unbound -> system.terminate()); // and shutdown when done
  }
  
  
  
  
}