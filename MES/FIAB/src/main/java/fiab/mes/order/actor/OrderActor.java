package fiab.mes.order.actor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.order.msg.CancelOrTerminateOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.restendpoint.requests.OrderHistoryRequest;
import fiab.mes.restendpoint.requests.OrderStatusRequest;
import fiab.tracing.actor.AbstractTracingActor;
import fiab.tracing.actor.messages.OrderStartMessage;

public class OrderActor extends AbstractTracingActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	protected String orderId;
	protected ActorSelection eventBusByRef;
	protected ActorSelection orderPlannerByRef;
	protected RegisterProcessRequest order;

	protected List<OrderEvent> history = new ArrayList<OrderEvent>();

	static public Props props(RegisterProcessRequest orderReq, ActorSelection eventBus,
			ActorSelection orderPlannerByRef) {
		return Props.create(OrderActor.class, () -> new OrderActor(orderReq, eventBus, orderPlannerByRef));
	}

	public OrderActor(RegisterProcessRequest orderReq, ActorSelection eventBusByRef, ActorSelection orderPlannerByRef) {
		super();

// This should be the first Trace in an order and it gets finished in the orderevent
		tracer.startConsumerSpan(new OrderStartMessage(), "Order actor created");

		System.out.println(tracer.getCurrentHeader());

		this.orderId = orderReq.getRootOrderId();
		this.order = orderReq;
		this.eventBusByRef = eventBusByRef;
		log.info("Subscribing to OrderEventBus for OrderId: " + orderId);
		eventBusByRef.tell(
				new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(self().path().name(), this.orderId)),
				getSelf());
		String msg = "Received RegisterProcessRequest, publish OrderEvent to create new Order with ID: " + orderId;
		OrderEvent createEvent = new OrderEvent(this.orderId, self().path().name(), OrderEventType.CREATED, msg);
		eventBusByRef.tell(createEvent, getSelf());
		history.add(createEvent);
		log.info("Forwarding order request to Planning Actor for OrderId: " + orderId);
		orderReq.setRequestor(getSelf());
		this.orderPlannerByRef = orderPlannerByRef;
		orderReq.setTracingHeader(tracer.getCurrentHeader());
		this.orderPlannerByRef.tell(orderReq, getSelf());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder() // needs refactoring to put behavior into separate methods
				.match(OrderStatusRequest.class, req -> {
					sender().tell(Optional.of(new OrderStatusRequest.Response(order.getProcess())), getSelf());
				}).match(OrderEvent.class, event -> {
					history.add(event);
					// OrderEvent rejected, canceled removed, premature_removal
					// add event type as tag/annotation

					switch (event.getEventType()) {
					case REJECTED:					
					case CANCELED:
					case REMOVED:
					case PREMATURE_REMOVAL:
						tracer.addAnnotation(event.getEventType().toString());
						tracer.finishCurrentSpan();
						break;
					default:
						break;
					}
				}).match(OrderHistoryRequest.class, req -> {
					List<OrderEvent> events = req.shouldResponseIncludeDetails() ? history
							: history.stream().map(event -> event.getCloneWithoutDetails())
									.collect(Collectors.toList());
					sender().tell(new OrderHistoryRequest.Response(orderId, events, req.shouldResponseIncludeDetails()),
							getSelf());
				}).match(CancelOrTerminateOrder.class, req -> {
					// forward to Planner to halt production and remove order from shopfloor
					try {
						tracer.startConsumerSpan(req, "Cancel Or terminate Order received");
						req.setTracingHeader(tracer.getCurrentHeader());
						orderPlannerByRef.tell(req, getSelf());
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						tracer.finishCurrentSpan();
					}
				}).build();
	}

}
