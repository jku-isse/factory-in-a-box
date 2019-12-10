package fiab.mes.transport.actor.transportmodule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;
import fiab.mes.transport.actor.transportmodule.wrapper.TransportModuleWrapperInterface;
import fiab.mes.transport.msg.TransportModuleRequest;


public class BasicTransportModuleActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected ActorSelection eventBusByRef;
	protected final AkkaActorBackedCoreModelAbstractActor machineId;
	protected AbstractCapability cap;
	protected MachineStatus currentState;
	protected TransportModuleWrapperInterface hal;
	protected InterMachineEventBus intraBus;
	
	protected TransportModuleRequest reservedForTReq = null;
	
	private List<MachineEvent> externalHistory = new ArrayList<MachineEvent>();
	//private List<MachineEvent> internalHistory = new ArrayList<MachineEvent>();
	
	static public Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, TransportModuleWrapperInterface hal, InterMachineEventBus intraBus) {	    
		return Props.create(BasicTransportModuleActor.class, () -> new BasicTransportModuleActor(machineEventBus, cap, modelActor, hal, intraBus));
	}
	
	public BasicTransportModuleActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, TransportModuleWrapperInterface hal, InterMachineEventBus intraBus) {
		this.cap = cap;
		this.machineId = new AkkaActorBackedCoreModelAbstractActor(modelActor.getID(), modelActor, self());
		this.eventBusByRef = machineEventBus;
		this.hal = hal;
		this.intraBus = intraBus;
		init();

	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		        .match(TransportModuleRequest.class, req -> {
		        	if (currentState.equals(MachineStatus.IDLE)) {
		        		setAndPublishSensedState(MachineStatus.STARTING);
		        		reservedForTReq = req;
		        		//TODO: ideally we would check if the requests directions encoded in the capabilities are indeed found on this transportmodule
		        		hal.transport(req);
		        	} else {
		        		//FIXME: respond with error message that we are not in the right state for request
		        	}
		        })
		        .match(MachineStatusUpdateEvent.class, mue -> {
		        	processMachineUpdateEvent(mue);
		        })
		        .match(MachineHistoryRequest.class, req -> {
		        	log.info(String.format("TransportModule %s received MachineHistoryRequest", machineId.getId()));
		        	List<MachineEvent> events = req.shouldResponseIncludeDetails() ? externalHistory :	externalHistory.stream().map(event -> event.getCloneWithoutDetails()).collect(Collectors.toList());
		        	MachineHistoryRequest.Response response = new MachineHistoryRequest.Response(machineId.getId(), events, req.shouldResponseIncludeDetails());
		        	sender().tell(response, getSelf());
		        })
		        .build();
	}

	private void init() {
		eventBusByRef.tell(new MachineConnectedEvent(machineId, Collections.singleton(cap), Collections.emptySet()), self());
		intraBus.subscribe(getSelf(), new SubscriptionClassifier(machineId.getId(), "*")); //ensure we get all events on this bus, but never our own, should we happen to accidentally publish some
		hal.subscribeToStatus();
	}
	
	private void processMachineUpdateEvent(MachineStatusUpdateEvent mue) {
		if (mue.getParameterName().equals(WellknownMachinePropertyFields.STATE_VAR_NAME)) {
			MachineStatus newState = mue.getStatus();
			setAndPublishSensedState(newState);
			switch(newState) {
			case COMPLETE:
				reservedForTReq = null;
				break;
			case COMPLETING:
				break;
			case EXECUTE:
				break;
			case IDLE:
				break;
			case RESETTING:
				break;
			case STARTING:
				break;
			case STOPPED:
				hal.reset(); // FIXME: how to handle, when we want to stop the whole shopfloor and not just immediately restart?!
				break;
			case STOPPING:
				break;
			default:
				break;
			}
		}
			
	}
	
	private void setAndPublishSensedState(MachineStatus newState) {
		String msg = String.format("%s sets state from %s to %s (Order: %s)", this.machineId.getId(), this.currentState, newState, reservedForTReq.getOrderId());
		log.debug(msg);
		if (currentState != newState) {
			this.currentState = newState;
			MachineUpdateEvent mue = new MachineStatusUpdateEvent(machineId.getId(), null, MachineOrderMappingManager.STATE_VAR_NAME, msg, newState);
			tellEventBus(mue);
		}
	}
	
	private void tellEventBus(MachineUpdateEvent mue) {
		externalHistory.add(mue);
		eventBusByRef.tell(mue, self());
	}
		
	
}
