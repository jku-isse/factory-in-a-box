package shopfloor.httpconnector;

import java.util.Optional;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import scala.concurrent.duration.FiniteDuration;
import shopfloor.agents.messages.FrontEndMessages.OrderStatusRequest;
import shopfloor.agents.messages.FrontEndMessages.OrderStatusResponse;
import shopfloor.agents.messages.OrderDocument;
import shopfloor.infra.ShopfloorDiscovery;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.pattern.PatternsCS.ask;

public class MinimalServerExample extends AllDirectives {

  private final ActorRef shopfloor;
	
  public static void main(String[] args) throws Exception {
    // boot up server using the route as defined below
    ActorSystem system = ActorSystem.create("routes");

    final Http http = Http.get(system);
    final ActorMaterializer materializer = ActorMaterializer.create(system);

    //In order to access all directives we need an instance where the routes are define.
    MinimalServerExample app = new MinimalServerExample(system);

    final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
    final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
        ConnectHttp.toHost("localhost", 8080), materializer);

    System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
    System.in.read(); // let it run until user presses return

    binding
        .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
        .thenAccept(unbound -> system.terminate()); // and shutdown when done
  }
  
  public MinimalServerExample(final ActorSystem system) {
	  shopfloor = system.actorOf(ShopfloorDiscovery.props());
  }

  private Route createRoute() {
	return concat(
		path("orders", () -> 
		    post(() ->	            
	              entity(Jackson.unmarshaller(OrderDocument.class), order -> {
	            	final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));
	            	CompletionStage<String> returnId = ask(shopfloor, order, timeout).thenApply((String.class::cast));
	            	return completeOKWithFuture(returnId, Jackson.marshaller());
	              }))				         
		 ),		
		get(() ->
			pathPrefix("order", () ->
				path(PathMatchers.remaining() , (String req) -> {								
				final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));			
				final CompletionStage<Optional<OrderStatusResponse>> futureMaybeStatus = ask(shopfloor, new OrderStatusRequest(req), timeout).thenApply((Optional.class::cast)); 
				return onSuccess(futureMaybeStatus, maybeStatus -> 
					maybeStatus.map( item -> completeOK(item, Jackson.marshaller()))
					.orElseGet(()-> complete(StatusCodes.NOT_FOUND, "Order Not Found"))
        	);	            
		})))
	);	    			    	
 }
  
}