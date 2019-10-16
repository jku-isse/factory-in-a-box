package fiab.mes.transport.actor.turntable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.UnsubscribeMessage;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.transport.actor.wrapper.ConveyorWrapper;
import fiab.mes.transport.actor.wrapper.ProcessEngineWrapper;
import fiab.mes.transport.actor.wrapper.TransportModuleWrapper;
import fiab.mes.transport.actor.wrapper.TurntableWrapper;
import fiab.mes.transport.messages.COM_Transport;
import fiab.mes.transport.messages.Reset;
import fiab.mes.transport.messages.Stop;

public class TransportModuleActor extends AbstractActor {
	
	private String id;
	private String orderId;
	private TransportModuleWrapper machineWrapper = null;
	private boolean connected;
	private long updateTimestamp;
	private Map<String, String> serverStates = new HashMap<String, String>();
	private ActorRef ttactor; //FU Turntable
	private ActorRef cvactor; //FU ConveyorBelt
	private ActorRef peactor; //FU ProcessEngine
	private ActorRef highLevelEventBusActor;

	public TransportModuleActor(String serverAddress, ActorSystem system) {
		this.machineWrapper = new TransportModuleWrapper(serverAddress);
		ttactor = system.actorOf(TurntableActor.props(getSelf(), new TurntableWrapper(machineWrapper)), id+"_TURNTABLE");
		cvactor = system.actorOf(ConveyorActor.props(getSelf(), new ConveyorWrapper(machineWrapper)), id+"_CONVEYOR");
		peactor = system.actorOf(ProcessEngineActor.props(getSelf(), new ProcessEngineWrapper(machineWrapper)), id+"_PROCESSENGINE");
		highLevelEventBusActor = system.actorOf(InterMachineEventBusWrapperActor.props(), InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		//TODO tell this.Actor to subscribe to hleb & oeb
	}

	public static Props props(String serverAddress, ActorSystem system) {
		return Props.create(TransportModuleActor.class, () -> new TransportModuleActor(serverAddress, system));
	}
	
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(String.class, string -> { //String should be completely removed at some point
					if (string.equals("GET_ID")) {
						getSender().tell(id, getSelf());
						
					} else if (string.equals("ping")) {
						
						machineWrapper.update("INSERT_SOME_NODE_ID"); //TODO
						getContext().system().scheduler().scheduleOnce(Duration.ofSeconds(10), getSelf(), "ping", getContext().system().dispatcher(), ActorRef.noSender());
						if(updateTimestamp - System.currentTimeMillis() > 60000) { //No updates for 30 seconds
							connected = false;
							//TODO add some orders, shutdown
						}
					}
				})
			.match(Stop.class, msg -> {
				machineWrapper.stop();
			})
			.match(Reset.class, msg -> {
				machineWrapper.reset();
			})
			.match(SubscribeMessage.class, msg -> {
				machineWrapper.subscribe(msg.getSubscriber(), msg.getSubscriptionClassifier().getTopic());
				serverStates.put(msg.getSubscriptionClassifier().getTopic(), "");
			})
			.match(UnsubscribeMessage.class, msg -> {
				if(msg.getSubscriptionClassifier().getTopic().equals("*")) {
					machineWrapper.unsubscribe(msg.getSubscriber());
				} else {
					machineWrapper.unsubscribe(msg.getSubscriber(), msg.getSubscriptionClassifier().getTopic());
				}
			})
			.match(MachineConnectedEvent.class, msg -> {
				
			})
			.match(MachineUpdateEvent.class, msg -> {
				//TODO proper message handling
				//This message is sent by the FUs, the messages have to be filtered before publishing them
				updateTimestamp = System.currentTimeMillis(); //Variable is used to check when last update was recieved
				serverStates.replace(msg.getParameterName(), msg.getNewValue().toString());
				getContext().system().scheduler().scheduleOnce(Duration.ofSeconds(10), getSelf(), "ping", getContext().system().dispatcher(), ActorRef.noSender());
				
				if(msg.getParameterName().equals("Machine_Status")) { //TODO the parameter might not be called Machine_Status
					highLevelEventBusActor.tell(msg.getNewValue(), getSelf());
				}
			})
			
			.match(COM_Transport.class, com -> {
				ttactor.tell(com, getSelf());
				cvactor.tell(com, getSelf());
				peactor.tell(com, getSelf());
			})

			.build();
	}


}
