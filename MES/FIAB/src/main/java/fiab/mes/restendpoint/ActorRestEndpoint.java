package fiab.mes.restendpoint;

import static akka.pattern.PatternsCS.ask;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.marshalling.sse.EventStreamMarshalling;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.model.sse.ServerSentEvent;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.directives.RouteAdapter;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import akka.util.Timeout;
import fiab.mes.auth.Authenticator;
import fiab.mes.auth.Authenticator.Credentials;
import fiab.mes.auth.Authenticator.User;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.machine.msg.MachineEventWrapper;
import fiab.mes.order.OrderProcess;
import fiab.mes.order.OrderProcessWrapper;
import fiab.mes.order.msg.CancelOrTerminateOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEventWrapper;
import fiab.mes.order.msg.OrderProcessUpdateEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;
import fiab.mes.restendpoint.requests.OrderHistoryRequest;
import fiab.mes.restendpoint.requests.OrderStatusRequest;
import fiab.mes.restendpoint.xmltransformer.EcoreStringUnmarshaller;
import scala.concurrent.duration.FiniteDuration;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.MachineEvent;

public class ActorRestEndpoint extends AllDirectives{

	private static final Logger logger = LoggerFactory.getLogger(ActorRestEndpoint.class);

	ActorSelection eventBusByRef;
	ActorSelection machineEventBusByRef;
	ActorRef orderEntryActor;
	ActorRef machineEntryActor;
	static boolean turnOffAuthenticate = true;
	static boolean turnOnAuthenticate = false;
	Authenticator auth;

	public ActorRestEndpoint(ActorSystem system, ActorRef orderEntryActor, ActorRef machineEntryActor) {
		this.eventBusByRef = system.actorSelection("/user/"+OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);	
		this.machineEventBusByRef = system.actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		this.orderEntryActor = orderEntryActor;
		this.machineEntryActor = machineEntryActor;		
		this.auth = new Authenticator(turnOnAuthenticate);
	}

	private static int bufferSize = 10;

	final RawHeader acaoAll = RawHeader.create("Access-Control-Allow-Origin", "*");
	final RawHeader acacEnable = RawHeader.create("Access-Control-Allow-Credentials", "true");
	final RawHeader aceh = RawHeader.create("Access-Control-Expose-Headers", "*");
	final RawHeader acah = RawHeader.create( "Access-Control-Allow-Headers", "*");	
	final List<HttpHeader> defaultCorsHeaders = Arrays.asList(acaoAll, aceh, acacEnable, acah);

	public Route createRoute() {
		return respondWithDefaultHeaders(defaultCorsHeaders, () ->
			concat(
				path("authenticate", () -> 
					concat( optionsAuth(), postAuth() )
				),
				path("action", () ->
					concat( optionsAction(), postAction() )
				),
				path("orderevents",	() -> 
					get(() -> parameterOptional("orderId", orderId -> getSSESourceForOrderEvents(orderId)))
				),
				path("machineEvents", () -> 
					get(() -> parameterOptional("machineId", machineId -> getSSESourceForMachineEvents(machineId)))
				),
				path("machines", () -> 
					concat( /*postMachines(),*/ getMachines(), options(() -> complete("This is a OPTIONS request.")) )
				),
				path("orders", () -> 
					concat( postOrders(), getOrders(), options(() -> complete("This is a OPTIONS request.")) )
				),
				pathPrefix("processevents",	() -> path(PathMatchers.remaining() , (String orderId) -> 
					concat( get(() -> getSSESourceForOrderProcessUpdateEvents(orderId)), options(() -> complete("This is a OPTIONS request.")) )
				)),	
				pathPrefix("order",	() -> path(PathMatchers.remaining() , (String req) -> 
					concat( get(() -> makeOrderStatusRequest(req)), options(() -> complete("This is a OPTIONS request.")) )
				)),	
				pathPrefix("orderHistory", () -> path(PathMatchers.remaining() , (String req) -> 
					concat( get(() -> makeOrderHistoryRequest(req)), options(() -> complete("This is a OPTIONS request.")) )
				)),
				pathPrefix("machineHistory", () -> path(PathMatchers.remaining() , (String req) -> 
					concat( get(() -> makeMachineHistoryRequest(req)), options(() -> complete("This is a OPTIONS request.")) )
				))
			)
		);	    			    	
	}

	private Route optionsAction() {
		
		return options(() -> complete("This is a OPTIONS request."));
	}
	
	private Route postAction() {
		return post(() -> entity(Jackson.unmarshaller(ActionRequest.class), req ->		
			headerValueByName("Authorization", token -> {
				if (auth.isLoggedIn(token)) {
						switch (req.getAction()) {
							case "stop":
								machineEntryActor.tell(new GenericMachineRequests.Stop(req.getId()), null);
								break;
							case "reset":
								machineEntryActor.tell(new GenericMachineRequests.Reset(req.getId()), null);
								break;
							case "delete":
								orderEntryActor.tell(new CancelOrTerminateOrder(null, req.getId()), null);
								break;
							default:
								logger.warn("Received invalid action request with action: \""+req.getAction()+"\"");
						}
					return complete(StatusCodes.ACCEPTED, req, Jackson.<ActionRequest>marshaller());
				}
				else {
					return complete(StatusCodes.UNAUTHORIZED);
				}
			})
		));
	}
	
	private Route optionsAuth() {
		return options(() -> complete("This is a OPTIONS request."));
	}
	
	private Route postAuth() {
		return post(() -> entity(Jackson.unmarshaller(Credentials.class), user -> {
			String username = user.getUsername();
			String password = user.getPassword();
            CompletionStage<Optional<User>> futureMaybeItem = CompletableFuture.completedFuture(auth.authenticate(username, password));
            return onSuccess(futureMaybeItem, maybeItem ->
	            maybeItem.map(item -> 
	            	respondWithHeader(RawHeader.create("Access-Token", item.getToken()), () ->
	            		completeOK( item.createPublicCopy() , Jackson.marshaller())
	            	)
	            ).orElseGet(() -> complete(StatusCodes.UNAUTHORIZED, "Username or password is incorrect"))
	        );
          }));
	}

//	private RegisterProcessRequest transformToOrderProcessRequest(String xmlPayload) {
//		throw new RuntimeException("Not implemented");
//	}
	
	private RouteAdapter getSSESourceForOrderEvents(Optional<String> orderId) {
		//final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));			
		//final CompletionStage<Optional<OrderStatusResponse>> futureMaybeStatus = ask(shopfloor, new OrderStatusRequest(orderId.orElse("")), timeout).thenApply((Optional.class::cast)); 
		//return onSuccess(futureMaybeStatus, maybeStatus -> 
		//	maybeStatus.map( item -> completeOK(item, Jackson.marshaller()))
		//	.orElseGet(()-> complete(StatusCodes.NOT_FOUND, "Order Not Found"))); 
		//}))
		logger.info("SSE (orderevent) requested with orderId: "+orderId.orElse("none provided"));
		Source<ServerSentEvent, NotUsed> source = 
				Source.actorRef(bufferSize, OverflowStrategy.dropHead())		
				.map(msg -> (OrderEvent) msg)
				.map(msg -> ServerSentEventTranslator.toServerSentEvent(msg))
				.mapMaterializedValue(actor -> { 
					eventBusByRef.tell(new SubscribeMessage(actor, new SubscriptionClassifier("RESTENDPOINT1", orderId.orElse("*"))) , actor);  
					return NotUsed.getInstance();
				});				
		return completeOK( source, EventStreamMarshalling.toEventStream());
	}
	
	private RouteAdapter getSSESourceForOrderProcessUpdateEvents(String orderId) {
		Optional<String> optId = Optional.of(orderId);
		logger.info("SSE (processevent) requested with orderId: "+optId.orElse("none provided"));
		Source<ServerSentEvent, NotUsed> source = 
				Source.actorRef(bufferSize, OverflowStrategy.dropHead())	
				.filter(msg -> msg instanceof OrderProcessUpdateEvent)
				.map(msg -> ServerSentEventTranslator.toServerSentEvent(((OrderProcessUpdateEvent)msg).getOrderId(), (OrderProcessUpdateEvent) msg))
				.mapMaterializedValue(actor -> { 
					eventBusByRef.tell(new SubscribeMessage(actor, new SubscriptionClassifier("RESTENDPOINT2", optId.orElse("*"))) , actor);
					return NotUsed.getInstance();
				});				
		return completeOK( source, EventStreamMarshalling.toEventStream());
	}
	
	private Route postOrders() {
		return post(() ->	  
			headerValueByName("Authorization", token -> {
				if (auth.isLoggedIn(token)) {
					return entity(EcoreStringUnmarshaller.unmarshaller(), request -> {
						final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));
						logger.info(String.format("REST Endpoint received Process request with DisplayName %s ", request.getDisplayName() ));						
						if (request.getID() == null) // if there was a problem in the XML
							return complete(StatusCodes.BAD_REQUEST, "Could not extract Process from XML Document");
						OrderProcess op = new OrderProcess(request);												
						// Id will be set by OrderEntryActor
						// Requestor will be set in OrderActor
						RegisterProcessRequest rpr = new RegisterProcessRequest("overwritten", op, ActorRef.noSender());												
						CompletionStage<String> returnId = ask(orderEntryActor, rpr, timeout).thenApply((String.class::cast));
						return completeOKWithFuture(returnId, Jackson.marshaller());
					});
//					return entity(Jackson.unmarshaller(String.class), orderAsXML -> { //TODO this needs to be an XML unmarshaller, not JSON!! 
//						final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));
//						// TODO: transform XML string into Ecore model: XML
//						RegisterProcessRequest order = transformToOrderProcessRequest(orderAsXML); // not sure how to do this yet
//						CompletionStage<String> returnId = ask(orderEntryActor, order, timeout).thenApply((String.class::cast));
//						return completeOKWithFuture(returnId, Jackson.marshaller());
//					});
				}
				return complete(StatusCodes.UNAUTHORIZED);
			})
		);
	}
	
	private Route getOrders() {
		return get(() -> {
			final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));		        
			@SuppressWarnings({ "unchecked", "deprecation" })
			CompletionStage<Set<OrderEventWrapper>> resp = ask(orderEntryActor, "GetAllOrders", timeout).thenApply( list -> {
				Set<OrderEventWrapper> wrap = ((Collection<OrderEvent>)list)
						.stream()
						.map(e -> new OrderEventWrapper(e))
						.collect(Collectors.toSet());
				return wrap;
			});
			return completeOKWithFuture(resp, Jackson.marshaller());
		});
	}
	
	private RouteAdapter makeOrderStatusRequest(String req) {
		final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));			
		final CompletionStage<Optional<OrderStatusRequest.Response>> futureMaybeStatus = ask(orderEntryActor, new OrderStatusRequest(req), timeout).thenApply((Optional.class::cast)); 
		return onSuccess(futureMaybeStatus, maybeStatus ->
			maybeStatus.map( item -> {
				OrderProcessWrapper wrapper = new OrderProcessWrapper(req, item);
				return completeOK(wrapper, Jackson.marshaller());
			})
			.orElseGet(()-> complete(StatusCodes.NOT_FOUND, "Order Not Found"))
		);
	}
	
	private RouteAdapter makeOrderHistoryRequest(String req) {
		final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));			
		final CompletionStage<OrderHistoryRequest.Response> futureMaybeStatus = ask(orderEntryActor, new OrderHistoryRequest(req), timeout)
				.thenApply(r -> (OrderHistoryRequest.Response) r); 
		return onSuccess(futureMaybeStatus, item -> {
			if (item != null) {
				if (item.getOrderId() == null) {
					return complete(StatusCodes.NOT_FOUND, "Order history Not Found");
				} else {
					List<OrderEventWrapper> wrapper = item.getUpdates().stream().map(o -> new OrderEventWrapper(o)).collect(Collectors.toList());
					return completeOK(wrapper, Jackson.marshaller());
				}
			} else {
				return complete(StatusCodes.NOT_FOUND, "Order history Not Found");
			}
		});
	}
	
	
	//----------------MACHINES-----------------------
	
	
	private RouteAdapter makeMachineHistoryRequest(String req) {
		final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));
		final CompletionStage<MachineHistoryRequest.Response> futureMaybeStatus = ask(machineEntryActor, new MachineHistoryRequest(req, true), timeout)
				.thenApply(r -> (MachineHistoryRequest.Response) r); 
		return onSuccess(futureMaybeStatus, item -> {
			if (item != null ) {
				if (item.getMachineId() == null)
					return complete(StatusCodes.NOT_FOUND, "Machine history Not Found");
				else {
					List<MachineEventWrapper> wrapper = item.getUpdates().stream().map(o -> new MachineEventWrapper(o)).collect(Collectors.toList());				
					return completeOK(wrapper, Jackson.marshaller());
				}
			} else {
				return complete(StatusCodes.NOT_FOUND, "Machine history Not Found");
			}
		});	
	}
	
//	private Route postMachines() {
//		return post(() ->
//			headerValueByName("Authorization", token -> {
//				if (auth.isLoggedIn(token)) {
//					return entity(Jackson.unmarshaller(String.class), orderAsXML -> { 
//						// TODO 
//						throw new RuntimeException("not implemented");
//					});
//				}
//				return complete(StatusCodes.UNAUTHORIZED);
//			})
//		);
//	}
	
	private Route getMachines() {
		return get(() -> {
			final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(5, TimeUnit.SECONDS));		        
			@SuppressWarnings({ "unchecked", "deprecation" })
			CompletionStage<Set<MachineEventWrapper>> resp = ask(machineEntryActor, "GetAllMachines", timeout).thenApply( list -> {
				Set<MachineEventWrapper> wrap = ((Collection<MachineEvent>)list)
						.stream()
						.map(e -> new MachineEventWrapper(e))
						.collect(Collectors.toSet());
				return wrap;
			});
			return completeOKWithFuture(resp, Jackson.marshaller());
		});
	}
	
	private RouteAdapter getSSESourceForMachineEvents(Optional<String> machineId) {
		logger.info("SSE (machineEvents) requested with machineId: "+machineId.orElse("none provided"));
		Source<ServerSentEvent, NotUsed> source = 
				Source.actorRef(bufferSize, OverflowStrategy.dropHead())		
				.map(msg -> (MachineEvent) msg)
				.map(msg -> ServerSentEventTranslator.toServerSentEvent(msg))
				.mapMaterializedValue(actor -> { 
					machineEventBusByRef.tell(new SubscribeMessage(actor, new SubscriptionClassifier("RESTENDPOINT", machineId.orElse("*"))) , actor);  
					return NotUsed.getInstance();
				});				
		return completeOK( source, EventStreamMarshalling.toEventStream());
	}
}
