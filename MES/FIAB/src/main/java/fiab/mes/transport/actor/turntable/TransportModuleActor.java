package fiab.mes.transport.actor.turntable;

import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import fiab.mes.transport.actor.wrapper.ConveyorWrapper;
import fiab.mes.transport.actor.wrapper.ProcessEngineWrapper;
import fiab.mes.transport.actor.wrapper.TransportModuleWrapper;
import fiab.mes.transport.actor.wrapper.TurntableWrapper;
import fiab.mes.transport.msg.COM_Transport;
import fiab.mes.transport.msg.MachineConnectedEvent;
import fiab.mes.transport.msg.MachineUpdateEvent;
import fiab.mes.transport.msg.SubscribeMessage;
import fiab.mes.transport.msg.UnsubscribeMessage;

public class TransportModuleActor extends AbstractActor {
	
	private String id;
	private TransportModuleWrapper machineWrapper = null;
	private int connectionFails;
	private boolean connected;
	private long updateTimestamp;
	private Map<String, String> serverStates = new HashMap<String, String>();
	private ActorRef ttactor;
	private ActorRef cvactor;
	private ActorRef peactor;
	private ActorRef highLevelEventBusActor;
	private ActorRef orderEventBusActor;

	public TransportModuleActor(String serverAddress, ActorSystem system) {
		this.machineWrapper = new TransportModuleWrapper(serverAddress);
		ttactor = system.actorOf(TurntableActor.props(getSelf(), new TurntableWrapper(machineWrapper)), id+"_TURNTABLE");
		cvactor = system.actorOf(ConveyorActor.props(getSelf(), new ConveyorWrapper(machineWrapper)), id+"_CONVEYOR");
		peactor = system.actorOf(ProcessEngineActor.props(getSelf(), new ProcessEngineWrapper(machineWrapper)), id+"_PROCESSENGINE");
		highLevelEventBusActor = system.actorSelection("akka://Actors/user/HighLevelEventBus").anchor();
		orderEventBusActor = system.actorSelection("akka://Actors/user/OrderEventBus").anchor();
		//TODO tell this.Actor to subscribe to hleb & oeb
	}

	public static Props props(String serverAddress, ActorSystem system) {
		return Props.create(TransportModuleActor.class, () -> new TransportModuleActor(serverAddress, system));
	}
	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(String.class, string -> {
					if(string.toLowerCase().equals("stop") && machineWrapper != null) {
						machineWrapper.stop();
					} else if (string.toLowerCase().equals("reset") && machineWrapper != null) {
						machineWrapper.reset();
					} else if (string.equals("GET_ID")) {
						getSender().tell(id, getSelf());
						
					} else if (string.equals("ping")) {
						if(updateTimestamp - System.currentTimeMillis() > 10000) { //No updates for 30 seconds
							connected = false;
							//TODO add some orders, shutdown
						}
					}
				})
			.match(SubscribeMessage.class, msg -> {
				machineWrapper.subscribe(msg.getSubscriber(), msg.getTopic());
				serverStates.put(msg.getTopic(), "");
			})
			.match(UnsubscribeMessage.class, msg -> {
				if(msg.getUnsubscribeActor()) {
					machineWrapper.unsubscribe(msg.getSubscriber());
				} else {
					machineWrapper.unsubscribe(msg.getSubscriber(), msg.getTopic());
				}
			})
			.match(MachineConnectedEvent.class, msg -> {
				
			})
			.match(MachineUpdateEvent.class, msg -> { //TODO proper message handling
				highLevelEventBusActor.tell(msg, getSelf());
				orderEventBusActor.tell(msg, getSelf());
			})
			
			.match(COM_Transport.class, com -> {
				ttactor.tell(com, getSelf());
				cvactor.tell(com, getSelf());
				peactor.tell(com, getSelf());
			})

			.build();
	}
	
	private void resetConnectionTimeOut() {
		connected = true;
		connectionFails = 0;
		updateTimestamp = System.currentTimeMillis();
	}


}
