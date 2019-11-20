package fiab.mes.machine.actor.plotter;

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
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;


public class BasicMachineActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected ActorSelection eventBusByRef;
	protected final AkkaActorBackedCoreModelAbstractActor machineId;
	protected AbstractCapability cap;
	protected MachineStatus currentState;
	protected PlottingMachineWrapperInterface hal;
	protected InterMachineEventBus intraBus;
	
	protected List<RegisterProcessStepRequest> orders = new ArrayList<>();
	protected RegisterProcessStepRequest reservedForOrder = null;
	
	private List<MachineEvent> externalHistory = new ArrayList<MachineEvent>();
	//private List<MachineEvent> internalHistory = new ArrayList<MachineEvent>();
	
	static public Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, PlottingMachineWrapperInterface hal, InterMachineEventBus intraBus) {	    
		return Props.create(BasicMachineActor.class, () -> new BasicMachineActor(machineEventBus, cap, modelActor, hal, intraBus));
	}
	
	public BasicMachineActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, PlottingMachineWrapperInterface hal, InterMachineEventBus intraBus) {
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
		        .match(RegisterProcessStepRequest.class, registerReq -> {
		        	orders.add(registerReq);
		        	log.info(String.format("Job %s of Order %s registered.", registerReq.getProcessStepId(), registerReq.getRootOrderId()));
		        	checkIfAvailableForNextOrder();
		        } )
		        .match(LockForOrder.class, lockReq -> {
		        	log.info("received LockForOrder msg "+lockReq.getStepId()+", current state: "+currentState);
		        	if (currentState == MachineStatus.IDLE) {
		        		hal.plot("", ""); // TODO pass on the correct values
		        		//TODO: here we assume correct invocation: thus on overtaking etc, will be improved later
		        	} else {
		        		log.warning("Received lock for order in state: "+currentState);
		        	}
		        })
		        .match(MachineUpdateEvent.class, mue -> {
		        	processMachineUpdateEvent(mue);
		        })
		        .match(MachineHistoryRequest.class, req -> {
		        	log.info(String.format("Machine %s received MachineHistoryRequest", machineId.getId()));
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
	
	private void processMachineUpdateEvent(MachineUpdateEvent mue) {
		if (mue.getParameterName().equals(WellknownMachinePropertyFields.STATE_VAR_NAME)) {
			MachineStatus newState = MachineStatus.valueOf(mue.getNewValue().toString());
			setAndPublishSensedState(newState);
			switch(newState) {
			case COMPLETE:
				break;
			case COMPLETING:
				break;
			case EXECUTE:
				break;
			case IDLE:
				checkIfAvailableForNextOrder();
				break;
			case RESETTING:
				break;
			case STARTING:
				break;
			case STOPPED:
				hal.reset();
				break;
			case STOPPING:
				break;
			default:
				break;
			}
		}
			
	}
	
	private void setAndPublishSensedState(MachineStatus newState) {
		String msg = String.format("%s sets state from %s to %s", this.machineId.getId(), this.currentState, newState);
		log.debug(msg);
		this.currentState = newState;
		MachineUpdateEvent mue = new MachineUpdateEvent(machineId.getId(), null, MachineOrderMappingManager.STATE_VAR_NAME, newState, msg);
		tellEventBus(mue);
	}
	
	private void tellEventBus(MachineUpdateEvent mue) {
		externalHistory.add(mue);
		eventBusByRef.tell(mue, self());
	}
	
	private void checkIfAvailableForNextOrder() {
		log.debug(String.format("Checking if %s is IDLE: %s", this.machineId.getId(), this.currentState));
		if (currentState == MachineStatus.IDLE && !orders.isEmpty() && reservedForOrder == null) { // if we are idle, tell next order to get ready, this logic is also triggered upon machine signaling completion
			RegisterProcessStepRequest ror = orders.remove(0);
			log.info("Ready for next Order: "+ror.getRootOrderId());
			reservedForOrder = ror; 
    		ror.getRequestor().tell(new ReadyForProcessEvent(ror), getSelf());
    	}	
	}	
	
}
