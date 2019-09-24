package shopfloor.infra;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import shopfloor.agents.eventbus.OrderEventBus;
import shopfloor.agents.impl.MachineAgent;
import shopfloor.agents.impl.OrderAgent;
import shopfloor.agents.messages.FrontEndMessages.OrderStatusRequest;
import shopfloor.agents.messages.OrderDocument;
import shopfloor.agents.messages.ProductionStateUpdate;
import shopfloor.agents.messages.ProductionStateUpdate.ProductionState;

public class ShopfloorDiscovery extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	private boolean isScanning = false;
	private HashMap<String, ActorRef> job2machineDict = new HashMap<>();
	private final AtomicInteger orderId = new AtomicInteger(0);
	private OrderEventBus eventBus;
	
	private HashMap<String, ActorRef> orderActors = new HashMap<>();
	private HashMap<String, ActorRef> machineActors = new HashMap<>();
	
	static public Props props(OrderEventBus eventBus) {
	    return Props.create(ShopfloorDiscovery.class, () -> new ShopfloorDiscovery(eventBus));
	}
	
	public ShopfloorDiscovery(OrderEventBus eventBus) {
		this.eventBus = eventBus;
		scanShopfloor();
	}
		

	@Override
	public Receive createReceive() {
		 return receiveBuilder()
			        .matchEquals(
			            "scan",
			            p -> {
			            	if (!isScanning) {
			            		isScanning = true;
			            		log.info("About to scan shopfloor");
			            		scanShopfloor();
			            		log.info("Finished scanning shopfloor");
			            		isScanning = false;
			            	} else {
			            		log.info("Still scanning, ignoring scan request");
			            	}			            		
			            })
			        .matchEquals("GetAllOrders", p -> {
			        	sender().tell(orderActors.keySet(), getSelf());
			        	// need to figure out how to collect order information from all order actors, perhaps a separate actors is needed for that
			        	})
			        .match(OrderDocument.class, doc -> {			        	
			        	String orderId = getNextOrderId();
			        	log.info("Processing Order with Id: "+orderId);
			        	sender().tell(orderId, getSelf());
			        	doc.setId(orderId);
			        	addOrder(doc);
			        })
			        .match(OrderStatusRequest.class, req -> {
			        	ActorRef oa = orderActors.get(req.getOrderId()) ;
			        	if (oa != null) {
			        		log.info("Forwarding Orderstatus Request");
			 				oa.forward(req, getContext());
			        	} else {
			        		log.info("OrderStatusRequest received for nonexisting order: "+req.getOrderId());
			        		sender().tell(Optional.empty(), getSelf());
			        	}
			        })
			        .matchAny(o -> log.info("Invalid message"))
			        .build();
	}
	
	protected void scanShopfloor() {
		final ActorRef m1 = getContext().actorOf(MachineAgent.props("Machine1"));
		machineActors.put("Machine1", m1);
		final ActorRef m2 = getContext().actorOf(MachineAgent.props("Machine2"));
		machineActors.put("Machine2", m2);
		
		job2machineDict.put("DrawTree", m1);
		job2machineDict.put("DrawSun", m2);
		job2machineDict.put("DrawLake", m1);
	}
	
	public Optional<ActorRef> getActorForOrderId(String orderId) {
		return Optional.ofNullable(this.orderActors.get(orderId));
	}
	
	public Optional<ActorRef> getActorForMachine(String capabilityId) {
		return Optional.ofNullable(this.machineActors.get(capabilityId));
	}
	
	private String getNextOrderId() {
		return "Order"+orderId.incrementAndGet();
	}
	
	protected void addOrder(OrderDocument orderDoc) {		
		final ActorRef orderActor = getContext().actorOf(OrderAgent.props(orderDoc, job2machineDict, eventBus));			
		orderActors.put(orderDoc.getId(), orderActor);
		orderActor.tell(new ProductionStateUpdate(ProductionState.COMPLETED, "INITJOB", Instant.now()), ActorRef.noSender());		
	}
	
}
