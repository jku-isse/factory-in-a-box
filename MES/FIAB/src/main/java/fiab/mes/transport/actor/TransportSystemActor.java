package fiab.mes.transport.actor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.OrderEventBusWrapperActor;
import fiab.mes.general.customDataTypes.TSAListElement;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.msg.OrderEvent;
import fiab.mes.order.msg.OrderEvent.OrderEventType;
import fiab.mes.transport.actor.turntable.TransportModuleActor;
import fiab.mes.transport.mockClasses.Direction;
import fiab.mes.transport.mockClasses.TEMP_TT_Connections;
import fiab.mes.transport.msg.COM_Transport;
import fiab.mes.transport.msg.RegisterTransportRequest;

public class TransportSystemActor extends AbstractActor {

	private List<TSAListElement> orders = new ArrayList<TSAListElement>();
	private Map<RegisterTransportRequest, String> processingOrders = new HashMap<RegisterTransportRequest, String>();
	private int transportId = 1;
	private ActorRef turntable1; //The actor of Turntable 1
	private ActorRef turntable2; //The actor of Turntable 2
	private ActorRef hlebActor; //HighLevelEventBus Actor
	private ActorRef oebActor; //OrderEventBus Actor
	private String statust1 = "IDLE"; //Status of Turntable 1
	private String statust2 = "IDLE"; //Status of Turntable 2
	private TEMP_TT_Connections t1connect = new TEMP_TT_Connections("TURNTABLE1", "PLOTTER1", "SOURCE", "PLOTTER2", "TURNTABLE2");
	private TEMP_TT_Connections t2connect = new TEMP_TT_Connections("TURNTABLE2", "PLOTTER3", "TARGET", "PLOTTER4", "TURNTABLE1");

	public TransportSystemActor(String id) {
		ActorSystem system = ActorSystem.create(id);
		hlebActor = system.actorOf(InterMachineEventBusWrapperActor.props(), "HighLevelEventBus");
		oebActor = system.actorOf(OrderEventBusWrapperActor.props(), "OrderEventBus");
		turntable1 = system.actorOf(TransportModuleActor.props("someaddress", system), "TURNTABLE1");
		turntable2 = system.actorOf(TransportModuleActor.props("someaddress", system), "TURNTABLE2");
		turntable1.tell(new String("Subscribe STATUS"), getSelf());
		turntable2.tell(new String("Subscribe STATUS"), getSelf());
	}
	

	public static Props props(String id) {
		return Props.create(TransportSystemActor.class, () -> new TransportSystemActor(id));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
		  .match(RegisterTransportRequest.class, order -> {
			orders.add(new TSAListElement(order, getSender()));
			handleOrder(orders.remove(0));
		}).match(MachineUpdateEvent.class, msg -> {
			if (getSender().equals(turntable1)) {
				statust1 = msg.getNewValue().toString();
			} else if (getSender().equals(turntable2)) {
				statust2 = msg.getNewValue().toString();
			}
		}).match(String.class, msg -> {
			if(msg.contains("Cancel Order: ")) { //TODO implement this properly
				String id = msg.substring(14);
				TSAListElement removethis = null;
				for(TSAListElement order: orders) {
					if(order.getRegisterTransportRequest().getId().equals(id)) {
						removethis = order;
					}
				}
				if(removethis != null) {
					orders.remove(removethis);
				}
				
				//TODO handle case where order was already sent out
			}
		})
		.match(OrderEvent.class, msg -> { //TODO is this the right way to proceed after an order being completed, canceled or deleted?
			for(Map.Entry<RegisterTransportRequest, String> mapEntry: processingOrders.entrySet()) {
				if(mapEntry.getKey().getId().equals(msg.getOrderId())) {
					OrderEventType type = msg.getEventType();
					if(type == OrderEventType.COMPLETED || type == OrderEventType.CANCELED || type == OrderEventType.DELETED ) {
						handleOrder(orders.remove(0));
					}
					processingOrders.replace(mapEntry.getKey(), ""); //TODO useful operation
				}
			}
		})
		.build();
	}
	
	private void handleOrder(TSAListElement tle) {
		boolean rejected = false;
		RegisterTransportRequest currentOrder = tle.getRegisterTransportRequest();
		ActorRef sender = tle.getSender();
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
		} else { //Handles rejected orders
			orders.add(0, tle); //Puts order at first place in the queue
		}
	}
	
	//Temporary
	//This return the position at which the machine is connected to the turntable
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

}
