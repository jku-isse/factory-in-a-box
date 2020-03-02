package fiab.mes.machine.actor.plotter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import ProcessCore.Parameter;
import ProcessCore.ProcessStep;
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
import fiab.mes.order.msg.ProcessRequestException;
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
		        	log.info(String.format("Received RegisterProcessStepRequest for order %s and step %s", registerReq.getRootOrderId(), registerReq.getProcessStepId()));
		        	registerRequest(registerReq);
		        } )
		        .match(LockForOrder.class, lockReq -> {
		        	log.info("received LockForOrder msg "+lockReq.getStepId()+", current state: "+currentState);
		        	plotUponLockForOrder(lockReq);
		        })
		        .match(CancelOrTerminateOrder.class, cto -> {
		        	handleOrderCancelRequest(cto);
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
	
	private void handleOrderCancelRequest(CancelOrTerminateOrder req) {
		log.info(String.format("Machine %s received CancelOrderRequest in state %s", machineId.getId(), currentState));
		// in what ever state, we remove it from the list of orders
		orders = orders.stream()
				.filter(rpsr -> !rpsr.getRootOrderId().equals(req.getRootOrderId()))
				.collect(Collectors.toList());		
		switch(currentState) {			
		case EXECUTE: // we just finish the order, hardcancel via a machine stop is available
			break;
		case IDLE:
			// check if current order is affected
			// currently we cancel at the level of root orders
			if (reservedForOrder.getRootOrderId().equals(req.getRootOrderId())) {
				reservedForOrder = null;
				checkIfAvailableForNextOrder();
			} 
			break;				
		case STARTING: // here we might not get the order transported incoming
		case COMPLETING:			
        	setAndPublishSensedState(MachineStatus.STOPPING);
        	hal.stop();
			// here we might not get the order transported away
			// thus we stop the machine
			doAutoResetAfterXseconds();
			break;										
		}
	}
	
	private void processMachineUpdateEvent(MachineStatusUpdateEvent mue) {
		if (mue.getParameterName().equals(WellknownMachinePropertyFields.STATE_VAR_NAME)) {
			MachineStatus newState = MachineStatus.valueOf(mue.getStatus().toString());
			setAndPublishSensedState(newState);
			switch(newState) {
			case COMPLETE:
				reservedForOrder = null;
				hal.reset(); //auto reset to become available for next round
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
	
	private void registerRequest(RegisterProcessStepRequest registerReq) {
		try {
			String ignoredHere = extractInputFromProcessStep(registerReq.getProcessStep());
			orders.add(registerReq);
	    	log.info(String.format("Job %s of Order %s registered.", registerReq.getProcessStepId(), registerReq.getRootOrderId()));
	    	checkIfAvailableForNextOrder();
		} catch (ProcessRequestException e) {
			log.warning("RegisterProcessStepRequest failed due to client error: "+e.getMessage());
			sender().tell(new ReadyForProcessEvent(registerReq, e), self());
		}
		
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
	
	private void plotUponLockForOrder(LockForOrder lockReq) {
		if (currentState == MachineStatus.IDLE) {
    		// we need to extract from the reservedOrder the step, and from the step the input properties		        	
			if (reservedForOrder != null || reservedForOrder.getProcessStepId().equals(lockReq.getStepId())) {
				String imgName = "demo";
				try {
					imgName = extractInputFromProcessStep(reservedForOrder.getProcessStep());
				} catch (ProcessRequestException e) {					
					e.printStackTrace();
					// this should not happen as we check before and only stored the request when there was no exception
				}
				hal.plot(imgName, lockReq.getRootOrderId()); 
    		//TODO: here we assume correct invocation order: thus order overtaking will be improved later
			} else {
				log.warning(String.format("No reserved order stored for LockForOrder %s request from %s", lockReq.toString(), sender().path().name()));
				sender().tell(new ReadyForProcessEvent(new RegisterProcessStepRequest(lockReq.getRootOrderId(), lockReq.getStepId(), null, sender()), false),  self); 
				// TODO: this should be a separate message type
			}			
    	} else {
    		String msg = "Received lock for order in non-IDLE state: "+currentState;
    		log.warning(msg);
    		sender().tell(new MachineInWrongStateResponse(this.machineId.getId(), WellknownMachinePropertyFields.STATE_VAR_NAME, msg, currentState, lockReq, MachineStatus.IDLE), self);
    	}
	}
	
	private String extractInputFromProcessStep(ProcessStep p) throws ProcessRequestException {
		if (p == null) throw new ProcessRequestException(ProcessRequestException.Type.PROCESS_STEP_MISSING, "Provided Process Step is null");
		if (p instanceof AbstractCapability) {
			AbstractCapability ac = ((AbstractCapability) p);
			if (!(ac.getUri().equals(cap.getUri()))) throw new ProcessRequestException(ProcessRequestException.Type.UNSUPPORTED_CAPABILITY, "Process Step Capability is not supported: "+ac.getUri());
			EList<Parameter> inputs = ac.getInputs();			
			if (inputs != null) {
				Optional<Parameter> optP = inputs.stream().filter(in -> in.getName().equals(WellknownPlotterCapability.PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME) )
				.findAny();
				if (optP.isPresent()) {
					if (optP.get().getValue() != null) {
						try {
							String param = (String)optP.get().getValue();
							return param;
						} catch (Exception e) {
							throw new ProcessRequestException(ProcessRequestException.Type.INPUT_PARAMS_MISSING_VALUE, "Capability missing value for input with name: "+WellknownPlotterCapability.PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME);
						}
					} else throw new ProcessRequestException(ProcessRequestException.Type.INPUT_PARAM_WRONG_TYPE, "Capability input value cannot be cast to String");
				} else throw new ProcessRequestException(ProcessRequestException.Type.STEP_MISSES_CAPABILITY, "Capability missing input with name: "+WellknownPlotterCapability.PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME);
			} else throw new ProcessRequestException(ProcessRequestException.Type.STEP_MISSES_CAPABILITY, "Capability missing any defined inputs");
		} else throw new ProcessRequestException(ProcessRequestException.Type.STEP_MISSES_CAPABILITY, "Process Step is not a capability");
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
	
	private void doAutoResetAfterXseconds() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofSeconds(3), 
    			 new Runnable() {
            @Override
            public void run() {
            	if (currentState.equals(MachineStatus.COMPLETE) 
            			|| currentState.equals(MachineStatus.STOPPED) ) {	        		
            		setAndPublishSensedState(MachineStatus.RESETTING); // not sensed, but machine would do the same (or fail, then we need to wait for machine to respond)            		
            		hal.reset();
            	} else if (currentState.equals(MachineStatus.COMPLETING) 
            			|| currentState.equals(MachineStatus.STOPPING) ) {
            		// we only recheck later of we are still in states leading to complete or stopped
            		doAutoResetAfterXseconds() ;          		
            	}
            }
          }, context().system().dispatcher());
	}

}
