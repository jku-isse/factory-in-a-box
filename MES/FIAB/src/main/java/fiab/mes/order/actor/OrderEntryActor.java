package fiab.mes.order.actor;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.MESSubscriptionClassifier;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.order.msg.CancelOrTerminateOrder;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.RegisterProcessRequest;
import fiab.mes.planer.actor.OrderPlanningActor;
import fiab.mes.restendpoint.requests.OrderHistoryRequest;
import fiab.mes.restendpoint.requests.OrderStatusRequest;
import fiab.tracing.actor.AbstractTracingActor;

public class OrderEntryActor extends AbstractTracingActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	public static final String WELLKNOWN_LOOKUP_NAME = "OrderEntryActor";

	private final AtomicInteger orderId = new AtomicInteger(0);
	private HashMap<String, ActorRef> orderActors = new HashMap<>();
	private HashMap<String, OrderEvent> latestChange = new HashMap<>();
	private ActorSelection eventBusByRef;
	private ActorSelection orderPlannerByRef;

	static public Props props() {
		return Props.create(OrderEntryActor.class, () -> new OrderEntryActor());
	}

	public OrderEntryActor() {
		eventBusByRef = context().actorSelection("/user/" + OrderEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		orderPlannerByRef = context().actorSelection("/user/" + OrderPlanningActor.WELLKNOWN_LOOKUP_NAME);
		eventBusByRef.tell(new SubscribeMessage(getSelf(), new MESSubscriptionClassifier(self().path().name(), "*")),
				getSelf());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().matchEquals("GetAllOrders", p -> {
			sender().tell(latestChange.values(), getSelf());
			// need to figure out how to collect order information from all order actors,
			// perhaps a separate actors is needed for that
		}).match(RegisterProcessRequest.class, doc -> {
			String orderId = getNextOrderId() + "#" + doc.getProcess().getProcess().getDisplayName();
			log.info("Processing Order with Id: " + orderId);
			sender().tell(orderId, getSelf());
			doc.setRootOrderId(orderId);
			addOrder(doc);
		}).match(OrderStatusRequest.class, req -> {
			ActorRef oa = orderActors.get(req.getOrderId());
			if (oa != null) {
				log.info("Forwarding Orderstatus Request");
				oa.forward(req, getContext());
			} else {
				log.info("OrderStatusRequest received for nonexisting order: " + req.getOrderId());
				sender().tell(Optional.empty(), getSelf());
			}
		}).match(OrderHistoryRequest.class, req -> {
			ActorRef oa = orderActors.get(req.getOrderId());
			if (oa != null) {
				log.info("Forwarding Orderstatus Request");
				oa.forward(req, getContext());
			} else {
				log.info("OrderHistoryRequest received for nonexisting order: " + req.getOrderId());
				sender().tell(new OrderHistoryRequest.Response(null, null, false), getSelf());
			}
		}).match(OrderEvent.class, event -> {
			try {
				tracer.startConsumerSpan(event, "Order Entry Actor: Order Event receied");
				this.latestChange.put(event.getOrderId(), event);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracer.finishCurrentSpan();
			}

		}).match(CancelOrTerminateOrder.class, req -> {
			try {
				tracer.startConsumerSpan(req, "Order Entry Actor: Cancel Or Terminate Order received");
				ActorRef oa = orderActors.get(req.getRootOrderId());
				if (oa != null) {
					log.info("Forwarding CancelOrTerminateOrder Request");
					oa.forward(req, getContext());
				} else {
					log.info("CancelOrTerminateOrder received for nonexisting order: " + req.getRootOrderId());
					sender().tell(Optional.empty(), getSelf());
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracer.finishCurrentSpan();
			}

		}).matchAny(o -> log.info("OrderEntryActor received Invalid message type: " + o.getClass().getSimpleName()))
				.build();
	}

	public Optional<ActorRef> getActorForOrderId(String orderId) {
		return Optional.ofNullable(this.orderActors.get(orderId));
	}

	private String getNextOrderId() {
		return "Order" + orderId.incrementAndGet();
	}

	protected void addOrder(RegisterProcessRequest orderDoc) {
		final ActorRef orderActor = getContext().actorOf(OrderActor.props(orderDoc, eventBusByRef, orderPlannerByRef));
		orderActors.put(orderDoc.getRootOrderId(), orderActor);
	}
}
