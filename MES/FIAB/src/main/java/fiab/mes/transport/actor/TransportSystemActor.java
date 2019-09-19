package fiab.mes.transport.actor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import fiab.mes.eventbus.HLEB_WrapperActor;
import fiab.mes.eventbus.OEB_WrapperActor;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.transport.actor.turntable.TransportModuleActor;
import fiab.mes.transport.mockClasses.Direction;
import fiab.mes.transport.mockClasses.TEMP_TT_Connections;
import fiab.mes.transport.msg.COM_Transport;
import fiab.mes.transport.msg.MachineUpdateEvent;
import fiab.mes.transport.msg.RegisterTransportRequest;

public class TransportSystemActor extends AbstractActor {

	private List<RegisterTransportRequest> orders = new ArrayList<RegisterTransportRequest>();
	private Map<RegisterTransportRequest, String> processingOrders = new HashMap<RegisterTransportRequest, String>();
	private int transportId = 1;
	private ActorRef turntable1;
	private ActorRef turntable2;
	private ActorRef hlebActor;
	private ActorRef oebActor;
	private String statust1 = "IDLE";
	private String statust2 = "IDLE";
	private boolean rejected = false;
	private TEMP_TT_Connections t1connect = new TEMP_TT_Connections("TURNTABLE1", "PLOTTER1", "SOURCE", "PLOTTER2", "TURNTABLE2");
	private TEMP_TT_Connections t2connect = new TEMP_TT_Connections("TURNTABLE2", "PLOTTER3", "TARGET", "PLOTTER4", "TURNTABLE1");

	public TransportSystemActor(String id) {
		ActorSystem system = ActorSystem.create(id);
		hlebActor = system.actorOf(HLEB_WrapperActor.props(), "HighLevelEventBus");
		oebActor = system.actorOf(OEB_WrapperActor.props(), "OrderEventBus");
		turntable1 = system.actorOf(TransportModuleActor.props("someaddress", system), "TURNTABLE1");
		turntable2 = system.actorOf(TransportModuleActor.props("someaddress", system), "TURNTABLE2");
		turntable1.tell(new String("Subscribe STATUS"), getSelf());
		turntable2.tell(new String("Subscribe STATUS"), getSelf());
	}
	
	private String getDirection(String turntable, String machine) {
		TEMP_TT_Connections connect;
		if(turntable.equals("TURNTABLE1")) {
			connect = t1connect;
		} else {
			connect = t2connect;
		}
		if(connect.getNorth().equals(machine)) {
			return "north";
		} else if(connect.getEast().equals(machine)) {
			return "east";
		} else if(connect.getSouth().equals(machine)) {
			return "south";
		} else if(connect.getWest().equals(machine)) {
			return "west";
		} else return null;
	}

	public static Props props(String id) {
		return Props.create(TransportSystemActor.class, () -> new TransportSystemActor(id));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(RegisterTransportRequest.class, order -> {
			orders.add(order);
			handleOrder(orders.remove(0), getSender());
			

		}).match(MachineUpdateEvent.class, msg -> {
			if (getSender().equals(turntable1)) {
				statust1 = msg.getMessage().toString();
			} else if (getSender().equals(turntable2)) {
				statust2 = msg.getMessage().toString();
			}
		}).match(String.class, msg -> {
			if(msg.contains("Cancel Order: ")) { //TODO implement this properly
				String id = msg.substring(14);
				RegisterTransportRequest removethis = null;
				for(RegisterTransportRequest order: orders) {
					if(order.getId().equals(id)) {
						removethis = order;
					}
				}
				if(removethis != null) {
					orders.remove(removethis);
				}
				
				//TODO handle case where order was already sent out
			}
		})
		.match(OrderEvent.class, msg -> {
			for(Map.Entry<RegisterTransportRequest, String> mapEntry: processingOrders.entrySet()) {
				if(mapEntry.getKey().getId().equals(msg.getOrderId())) {
					processingOrders.replace(mapEntry.getKey(), ""); //TODO useful operation
				}
			}
		})
		.build();
	}
	
	private void handleOrder(RegisterTransportRequest currentOrder, ActorRef sender) {

		// Checking if order can be taken
		if (currentOrder.getFromMachine().equals("TURNTABLE1")
				|| currentOrder.getToMachine().equals("TURNTABLE1")) {
			if (!statust1.equals("IDLE")) {
				rejected = true;
			}
		}
		if (currentOrder.getFromMachine().equals("TURNTABLE2")
				|| currentOrder.getToMachine().equals("TURNTABLE2")) {
			if (!statust2.equals("IDLE")) {
				rejected = true;
			}
		}
		if (!rejected) {
			processingOrders.put(currentOrder, "ORDER_PROCESSING");
			currentOrder.setTransportId(String.valueOf(transportId));
			sender.tell(currentOrder, getSelf());
			transportId++;
			if (currentOrder.getFromMachine().equals("TURNTABLE1")
					&& currentOrder.getToMachine().equals("TURNTABLE2")) {
				//The value YOU is set because there is no information at the moment where it should go
				//The value has to be replaced once the Transport Route is done
				turntable1.tell(new COM_Transport(new Direction("YOU"), new Direction("EAST"), currentOrder.getId()), getSelf());
				turntable2.tell(new COM_Transport(new Direction("WEST"), new Direction("YOU"), currentOrder.getId()), getSelf());

			} else if (currentOrder.getFromMachine().equals("TURNTABLE2")
					&& currentOrder.getToMachine().equals("TURNTABLE1")) {
				turntable1.tell(new COM_Transport(new Direction("WEST"), new Direction("YOU"), currentOrder.getId()), getSelf());
				turntable2.tell(new COM_Transport(new Direction("YOU"), new Direction("WEST"), currentOrder.getId()), getSelf());
				
			} else if (currentOrder.getFromMachine().equals("TURNTABLE1")) {
				turntable1.tell(new COM_Transport(new Direction("YOU"), new Direction(getDirection("TURNTABLE1", currentOrder.getToMachine())), currentOrder.getId()), getSelf());
				
			} else if (currentOrder.getFromMachine().equals("TURNTABLE2")) {
				turntable2.tell(new COM_Transport(new Direction("YOU"), new Direction(getDirection("TURNTABLE2", currentOrder.getToMachine())), currentOrder.getId()), getSelf());
				
			} else if (currentOrder.getToMachine().equals("TURNTABLE1")) {
				turntable1.tell(new COM_Transport(new Direction(getDirection("TURNTABLE1", currentOrder.getFromMachine())), new Direction("YOU"), currentOrder.getId()), getSelf());
				
			} else if (currentOrder.getToMachine().equals("TURNTABLE2")) {
				turntable2.tell(new COM_Transport(new Direction(getDirection("TURNTABLE2", currentOrder.getFromMachine())), new Direction("YOU"), currentOrder.getId()), getSelf());
				
			}
		} else {
			//TODO handle rejected Orders
			//Something someting
		}
	}

}
