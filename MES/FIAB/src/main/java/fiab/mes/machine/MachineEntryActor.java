package fiab.mes.machine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBusWrapperActor;
import fiab.mes.eventbus.SubscribeMessage;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.machine.msg.GenericMachineRequests;
import fiab.mes.machine.msg.GenericMachineRequests.BaseRequest;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;

public class MachineEntryActor extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	public static final String WELLKNOWN_LOOKUP_NAME = "MachineEntryActor";
	
	private ActorSelection machineEventBusByRef;
	private Map<String, MachineEvent> latestChange = new HashMap<>();
	private Map<String, ActorRef> machineActors = new HashMap<>();
	
	static public Props props() {
	    return Props.create(MachineEntryActor.class, () -> new MachineEntryActor());
	}
	
	public MachineEntryActor() {
		machineEventBusByRef = context().actorSelection("/user/"+InterMachineEventBusWrapperActor.WRAPPER_ACTOR_LOOKUP_NAME);
		machineEventBusByRef.tell(new SubscribeMessage(getSelf(), new SubscriptionClassifier(self().path().name(), "*")), getSelf());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.matchEquals("GetAllMachines", m -> {
					sender().tell(latestChange.values(), getSelf());
				})
				.match(MachineHistoryRequest.class, req -> {
					forwardToMachineActor(req);
				})
				.match(MachineConnectedEvent.class, me -> {
					machineActors.put(me.getMachine().getId(), me.getMachine().getAkkaActor());
				})
				.match(MachineDisconnectedEvent.class, me -> {
					machineActors.remove(me.getMachine().getId());
				})
				.match(MachineEvent.class, e -> {
					latestChange.put(e.getMachineId(), e);
				})
				.match(GenericMachineRequests.BaseRequest.class, req -> {
					forwardBaseRequestToMachineActor(req);
				})
				.matchAny(o -> log.info("MachineEntryActor received Invalid message type: "+o.getClass().getSimpleName()))
				.build();
	}
	
	private void forwardToMachineActor(MachineHistoryRequest req) {
		ActorRef oa = machineActors.get(req.getMachineId());
		if (oa != null) {
			log.info("Forwarding MachineHistoryRequest");
			oa.forward(req, getContext());
		} else {
			log.warning(String.format("Received MachineHistoryRequest for non-existing machine: %s", req.getMachineId()));
			sender().tell(Optional.empty(), getSelf());
		}
	}
	
	private void forwardBaseRequestToMachineActor(BaseRequest req) {
		ActorRef oa = machineActors.get(req.getMachineId());
		if (oa != null) {
			log.debug("Forwarding BaseRequest to "+req.getMachineId());
			oa.forward(req, getContext());
		} else {
			log.warning(String.format("Received BaseRequest for non-existing machine: %s", req.getMachineId()));
			sender().tell(Optional.empty(), getSelf());
		}
	}
}