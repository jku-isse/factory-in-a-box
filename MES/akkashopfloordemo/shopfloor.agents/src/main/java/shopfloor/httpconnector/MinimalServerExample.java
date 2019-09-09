package shopfloor.httpconnector;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.EventBus;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.model.sse.ServerSentEvent;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.marshalling.sse.EventStreamMarshalling;
import akka.stream.ActorMaterializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.Timeout;
import scala.concurrent.duration.FiniteDuration;
import shopfloor.agents.eventbus.OrderEventBus;
import shopfloor.agents.events.OrderEvent;
import shopfloor.agents.messages.FrontEndMessages.OrderStatusRequest;
import shopfloor.agents.messages.FrontEndMessages.OrderStatusResponse;
import shopfloor.agents.messages.OrderDocument;
import shopfloor.infra.ShopfloorDiscovery;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  
  private static final Logger logger = LoggerFactory.getLogger(MinimalServerExample.class);
  private OrderEventBus orderEvents = new OrderEventBus();
  
  public MinimalServerExample(final ActorSystem system) {
	  shopfloor = system.actorOf(ShopfloorDiscovery.props(orderEvents));
  }
  
  private static int bufferSize = 10;
  
  final RawHeader acaoAll = RawHeader.create("Access-Control-Allow-Origin", "*");
  final RawHeader acacEnable = RawHeader.create("Access-Control-Allow-Credentials", "true");
  final RawHeader aceh = RawHeader.create("Access-Control-Expose-Headers", "*");
  final RawHeader acah = RawHeader.create( "Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
  final List<HttpHeader> defaultCorsHeaders = Arrays.asList(acaoAll, aceh, acacEnable, acah);
  
  private Route createRoute() {
	return respondWithDefaultHeaders(defaultCorsHeaders, () ->
	  concat(
		path("events", () -> 
			get(() -> parameterOptional("orderId", orderId -> {								
					//final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));			
					//final CompletionStage<Optional<OrderStatusResponse>> futureMaybeStatus = ask(shopfloor, new OrderStatusRequest(orderId.orElse("")), timeout).thenApply((Optional.class::cast)); 
					//return onSuccess(futureMaybeStatus, maybeStatus -> 
					//	maybeStatus.map( item -> completeOK(item, Jackson.marshaller()))
					//	.orElseGet(()-> complete(StatusCodes.NOT_FOUND, "Order Not Found"))); 
					//}))
				logger.info("SSE requested with orderId: "+orderId.orElse("none provided"));
				Source<ServerSentEvent, NotUsed> source = 
						Source.actorRef(bufferSize, OverflowStrategy.dropHead())		
						.map(msg -> (OrderEvent) msg)
						.map(msg -> ServerSentEventTranslator.toServerSentEvent(msg) )
						.mapMaterializedValue(actor -> { orderEvents.subscribe(actor, orderId.orElse("*")); 
														 return NotUsed.getInstance();
														});				
				return completeOK( source, EventStreamMarshalling.toEventStream());
				
					
					
				//return completeOK(orderId.orElse("All"), Jackson.marshaller());	
				//Source stream;
				//	Source<Out, Mat>.fromFuture(futureMaybeStatus)
				//	completeOk();
			}))
			),			
		path("orders", () -> concat( 
		    post(() ->	            
	              entity(Jackson.unmarshaller(OrderDocument.class), order -> {
	            	final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));
	            	CompletionStage<String> returnId = ask(shopfloor, order, timeout).thenApply((String.class::cast));
	            	return completeOKWithFuture(returnId, Jackson.marshaller());
	              })),
		    get(() -> {
		    	final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));		        
		        CompletionStage<Set<String>> resp = ask(shopfloor, "GetAllOrders", timeout).thenApply( list -> (Set<String>)list);
		        return completeOKWithFuture(resp, Jackson.marshaller());
		    })
		 )),		
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
	));	    			    	
 }
  
}