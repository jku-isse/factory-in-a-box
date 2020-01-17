package fiab.mes.machine.actor.plotter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.eventbus.SubscriptionClassifier;
import fiab.mes.general.HistoryTracker;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineInWrongStateResponse;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.machine.msg.GenericMachineRequests.Reset;
import fiab.mes.machine.msg.GenericMachineRequests.Stop;
import fiab.mes.order.msg.CancelOrTerminateOrder;
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
	private String lastOrder;
	private ActorRef self;
	protected RegisterProcessStepRequest reservedForOrder = null;
	
	protected HistoryTracker externalHistory=null;
	//private List<MachineEvent> externalHistory = new ArrayList<MachineEvent>();
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
		this.self = self();
		this.externalHistory = new HistoryTracker(machineId.getId());
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
		        		//TODO: here we assume correct invocation: thus order overtaking etc, will be improved later
		        	} else {
		        		String msg = "Received lock for order in non-IDLE state: "+currentState;
		        		log.warning(msg);
		        		sender().tell(new MachineInWrongStateResponse(this.machineId.getId(), WellknownMachinePropertyFields.STATE_VAR_NAME, msg, currentState, lockReq, MachineStatus.IDLE), self);
		        	}
		        })
		        .match(CancelOrTerminateOrder.class, cto -> {
		        	//TODO: implement
		        })
		        .match(Stop.class, req -> {
		        	log.info(String.format("Machine %s received StopRequest", machineId.getId()));
		        	setAndPublishSensedState(MachineStatus.STOPPING);
		        	hal.stop();
		        })
		        .match(Reset.class, req -> {
		        	if (currentState.equals(MachineStatus.COMPLETE) 
		        			|| currentState.equals(MachineStatus.STOPPED) ) {
		        		log.info(String.format("Machine %s received ResetRequest in suitable state", machineId.getId()));
		        		setAndPublishSensedState(MachineStatus.RESETTING); // not sensed, but machine would do the same (or fail, then we need to wait for machine to respond)
		        		hal.reset();
		        	} else {
		        		log.warning(String.format("Machine %s received ResetRequest in non-COMPLETE or non-STOPPED state, ignoring", machineId.getId()));
		        	}
		        })
		        .match(MachineStatusUpdateEvent.class, mue -> {
		        	processMachineUpdateEvent(mue);
		        })
		        .match(MachineHistoryRequest.class, req -> {
		        	log.info(String.format("Machine %s received MachineHistoryRequest", machineId.getId()));
		        	externalHistory.sendHistoryResponseTo(req, getSender(), self);
		        })
		        .build();
	}

	private void init() {
		eventBusByRef.tell(new MachineConnectedEvent(machineId, Collections.singleton(cap), Collections.emptySet()), self);
		intraBus.subscribe(self, new SubscriptionClassifier(machineId.getId(), "*")); //ensure we get all events on this bus, but never our own, should we happen to accidentally publish some
		hal.subscribeToStatus();
	}
	
	private void processMachineUpdateEvent(MachineStatusUpdateEvent mue) {
		if (mue.getParameterName().equals(WellknownMachinePropertyFields.STATE_VAR_NAME)) {
			MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
			setAndPublishSensedState(newState);
			switch(newState) {
			case COMPLETE:
				reservedForOrder = null;
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
				break;
			case STOPPING:
				break;
			default:
				break;
			}
		}
			
	}
	
	private void setAndPublishSensedState(MachineStatus newState) {
		String msg = String.format("%s sets state from %s to %s (Order: %s)", this.machineId.getId(), this.currentState, newState, lastOrder);
		log.debug(msg);
		this.currentState = newState;
		MachineUpdateEvent mue = new MachineStatusUpdateEvent(machineId.getId(), null, WellknownMachinePropertyFields.STATE_VAR_NAME, msg, newState);
		tellEventBus(mue);
	}
	
	private void checkIfAvailableForNextOrder() {
		log.debug(String.format("Checking if %s is IDLE: %s", this.machineId.getId(), this.currentState));
		if (currentState == MachineStatus.IDLE && !orders.isEmpty() && reservedForOrder == null) { // if we are idle, tell next order to get ready, this logic is also triggered upon machine signaling completion
			RegisterProcessStepRequest ror = orders.remove(0);
			lastOrder = ror.getRootOrderId();
			log.info("Ready for next Order: "+ror.getRootOrderId());
			reservedForOrder = ror; 
    		ror.getRequestor().tell(new ReadyForProcessEvent(ror), getSelf());
    	}	
	}	
	
	private void tellEventBus(MachineUpdateEvent mue) {
		externalHistory.add(mue);
		tellEventBusWithoutAddingToHistory(mue);
		lastMUE=mue;
		resendLastEvent();
	}
	
	private void tellEventBusWithoutAddingToHistory(MachineUpdateEvent mue) {
		eventBusByRef.tell(mue, self);
	}
		
	private MachineUpdateEvent lastMUE;
	
	private void resendLastEvent() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000*10), 
    			 new Runnable() {
            @Override
            public void run() {
            	tellEventBusWithoutAddingToHistory(lastMUE);
            }
          }, context().system().dispatcher());
	}
}
