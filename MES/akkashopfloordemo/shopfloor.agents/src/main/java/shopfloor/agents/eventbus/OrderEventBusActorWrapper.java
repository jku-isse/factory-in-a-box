package shopfloor.agents.eventbus;


import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import shopfloor.agents.events.OrderEvent;



public class OrderEventBusActorWrapper  extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected OrderEventBus eventBus;
	
	static public Props props(OrderEventBus eventBus) {	    
		return Props.create(OrderEventBusActorWrapper.class, () -> new OrderEventBusActorWrapper(eventBus));
	  }
	
	private OrderEventBusActorWrapper(OrderEventBus eventBus) {
		this.eventBus = eventBus;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(OrderEvent.class, event -> {
        	eventBus.publish(event);
        })
        .matchAny(o -> log.warning("Unsupported message of type: "+o.getClass().getSimpleName()))
        .build();
	}

}
