package fiab.mes.order.actor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.restendpoint.requests.OrderStatusRequest;
import fiab.mes.restendpoint.requests.OrderHistoryRequest;

public class OrderActor extends AbstractActor{
	
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	protected String orderId;	
	protected ActorSelection eventBusByRef;
	protected ActorSelection orderPlannerByRef;
	protected RegisterProcessRequest order;
	
	protected List<OrderEvent> history = new ArrayList<OrderEvent>();
	
	static public Props props(RegisterProcessRequest orderReq, ActorSelection eventBus, ActorSelection orderPlannerByRef) {	    
		return Props.create(OrderActor.class, () -> new OrderActor(orderReq, eventBus, orderPlannerByRef));
	  }
	
	public OrderActor(RegisterProcessRequest orderReq, ActorSelection eventBusByRef, ActorSelection orderPlannerByRef) {
		this.orderId = orderReq.getRootOrderId();
		this.order = orderReq;
		this.eventBusByRef = eventBusByRef;
		log.info("Subscribing to OrderEventBus for OrderId: "+orderId);
		eventBusByRef.tell(new SubscribeMessage(getSelf(), new SubscriptionClassifier(self().path().name(), this.orderId)), getSelf() );
		OrderEvent createEvent = new OrderEvent(this.orderId, self().path().name(), OrderEventType.CREATED);
		eventBusByRef.tell(createEvent, getSelf());
		history.add(createEvent);
		log.info("Forwarding order request to Planning Actor for OrderId: "+orderId);
		orderReq.setRequestor(getSelf());
		orderPlannerByRef.tell(orderReq, getSelf());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder() // needs refactoring to put behavior into separate methods
		        .match(OrderStatusRequest.class, req -> {
		        	sender().tell(Optional.of(new OrderStatusRequest.Response(order.getProcess())), getSelf());
		        })
		        .match(OrderEvent.class, event -> {
		        	history.add(event);		        	
		        })
		        .match(OrderHistoryRequest.class, req -> {
		        	List<OrderEvent> events = req.shouldResponseIncludeDetails() ? history :	history.stream().map(event -> event.getCloneWithoutDetails()).collect(Collectors.toList());
		        	sender().tell(new OrderHistoryRequest.Response(orderId, events, req.shouldResponseIncludeDetails()), getSelf());
		        })
		        .build();
	}
	
}
 