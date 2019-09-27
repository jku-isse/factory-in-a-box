package fiab.mes.restendpoint;

import static akka.pattern.PatternsCS.ask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ProcessCore.XmlRoot;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.marshalling.sse.EventStreamMarshalling;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.model.sse.ServerSentEvent;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import akka.util.Timeout;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.order.OrderProcessWrapper;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEventWrapper;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.restendpoint.requests.OrderStatusRequest;
import scala.concurrent.duration.FiniteDuration;

public class ActorRestEndpoint extends AllDirectives{

	private static final Logger logger = LoggerFactory.getLogger(ActorRestEndpoint.class);

	ActorSelection eventBusByRef;
	ActorRef orderEntryActor;

	public ActorRestEndpoint(ActorSystem system, ActorRef orderEntryActor) {
		eventBusByRef = system.actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);	
		this.orderEntryActor = orderEntryActor;
	}

	private static int bufferSize = 10;

	final RawHeader acaoAll = RawHeader.create("Access-Control-Allow-Origin", "*");
	final RawHeader acacEnable = RawHeader.create("Access-Control-Allow-Credentials", "true");
	final RawHeader aceh = RawHeader.create("Access-Control-Expose-Headers", "*");
	final RawHeader acah = RawHeader.create( "Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
	final List<HttpHeader> defaultCorsHeaders = Arrays.asList(acaoAll, aceh, acacEnable, acah);

	protected Route createRoute() {
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
								.mapMaterializedValue(actor -> { 
									eventBusByRef.tell(new SubscribeMessage(actor, new SubscriptionClassifier("RESTENDPOINT", orderId.orElse("*"))) , actor);  
									return NotUsed.getInstance();
								});				
						return completeOK( source, EventStreamMarshalling.toEventStream());
					}))
				),		
				path("orders", () -> concat( 
					post(() ->	            
						entity(Jackson.unmarshaller(String.class), orderAsXML -> { //TODO this needs to be an XML unmarshaller, not JSON!! 
							final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));
							// TODO: transform XML string into Ecore model: XML
							RegisterProcessRequest order = transformToOrderProcessRequest(orderAsXML); // not sure how to do this yet
							CompletionStage<String> returnId = ask(orderEntryActor, order, timeout).thenApply((String.class::cast));
							return completeOKWithFuture(returnId, Jackson.marshaller());
						})
					),
					get(() -> {
						final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));		        
						CompletionStage<Set<OrderEvent>> resp = ask(orderEntryActor, "GetAllOrders", timeout).thenApply( list -> (Set<OrderEvent>)list);
						//TODO wrapping doesn't work!!!
						CompletionStage<Set<OrderEventWrapper>> respW = resp.thenApply(r -> (Set<OrderEventWrapper>) r.stream().collect(Collectors.toMap(o -> o.getOrderId(), o -> new OrderEventWrapper(o))));
						return completeOKWithFuture(respW, Jackson.marshaller());
					})
				)),	
				get(() -> 			
					pathPrefix("order", () ->
						path(PathMatchers.remaining() , (String req) -> {								
							final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));			
							final CompletionStage<Optional<OrderStatusRequest.Response>> futureMaybeStatus = ask(orderEntryActor, new OrderStatusRequest(req), timeout).thenApply((Optional.class::cast)); 
							return onSuccess(futureMaybeStatus, maybeStatus ->
								maybeStatus.map( item -> {
									OrderProcessWrapper wrapper = new OrderProcessWrapper(item);
									return completeOK(wrapper, Jackson.marshaller());
								})
								.orElseGet(()-> complete(StatusCodes.NOT_FOUND, "Order Not Found"))
							);	            
						})
					)
				)
			)
		);	    			    	
	}

	private RegisterProcessRequest transformToOrderProcessRequest(String xmlPayload) {
		throw new RuntimeException("Not implemented");
	}
}