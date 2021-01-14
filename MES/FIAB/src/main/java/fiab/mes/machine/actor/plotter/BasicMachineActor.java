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
import ProcessCore.CapabilityInvocation;
import ProcessCore.Parameter;
import ProcessCore.ProcessStep;
import ProcessCore.VariableMapping;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import main.java.fiab.core.capabilities.BasicMachineStates;
import main.java.fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import main.java.fiab.core.capabilities.basicmachine.events.MachineInWrongStateResponse;
import main.java.fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import main.java.fiab.core.capabilities.basicmachine.events.MachineUpdateEvent;
import main.java.fiab.core.capabilities.plotting.WellknownPlotterCapability;
import fiab.machine.plotter.IntraMachineEventBus;
import fiab.machine.plotter.SubscriptionClassifier;
import fiab.mes.general.HistoryTracker;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.plotter.wrapper.PlottingMachineWrapperInterface;
import fiab.mes.machine.msg.GenericMachineRequests.Reset;
import fiab.mes.machine.msg.GenericMachineRequests.Stop;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineDisconnectedEvent;
import fiab.mes.order.msg.CancelOrTerminateOrder;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ProcessRequestException;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;


public class BasicMachineActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected ActorSelection eventBusByRef;
	protected final AkkaActorBackedCoreModelAbstractActor machineId;
	protected AbstractCapability cap;
	protected BasicMachineStates currentState;
	protected PlottingMachineWrapperInterface hal;
	protected IntraMachineEventBus intraBus;
	
	protected List<RegisterProcessStepRequest> orders = new ArrayList<>();
	private String lastOrder;
	private ActorRef self;
	protected RegisterProcessStepRequest reservedForOrder = null;
	
	protected HistoryTracker externalHistory=null;
	//private List<MachineEvent> externalHistory = new ArrayList<MachineEvent>();
	//private List<MachineEvent> internalHistory = new ArrayList<MachineEvent>();
	
	static public Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, PlottingMachineWrapperInterface hal, IntraMachineEventBus intraBus) {	    
		return Props.create(BasicMachineActor.class, () -> new BasicMachineActor(machineEventBus, cap, modelActor, hal, intraBus));
	}
	
	public BasicMachineActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, PlottingMachineWrapperInterface hal, IntraMachineEventBus intraBus) {
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
		        	setAndPublishSensedState(BasicMachineStates.STOPPING);
		        	hal.stop();
		        })
		        .match(Reset.class, req -> {
		         	if (currentState.equals(BasicMachineStates.COMPLETE) 
		        			|| currentState.equals(BasicMachineStates.STOPPED) ) {
		        		log.info(String.format("Machine %s received ResetRequest in suitable state", machineId.getId()));
		        		setAndPublishSensedState(BasicMachineStates.RESETTING); // not sensed, but machine would do the same (or fail, then we need to wait for machine to respond)
		        		reset();
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
		        .match(MachineDisconnectedEvent.class, req -> {					
		        	log.warning(String.format("Lost connection to machine in state: %s, sending disconnected event and shutting down actor", this.currentState));		        	
					eventBusByRef.tell(new MachineDisconnectedEvent(machineId), self());					
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
        	setAndPublishSensedState(BasicMachineStates.STOPPING);
        	hal.stop();
			// here we might not get the order transported away
			// thus we stop the machine
			doAutoResetAfterXseconds();
			break;										
		}
	}
	
	private void processMachineUpdateEvent(MachineStatusUpdateEvent mue) {
		if (mue.getParameterName().equals(OPCUABasicMachineBrowsenames.STATE_VAR_NAME)) {
			BasicMachineStates newState = BasicMachineStates.valueOf(mue.getStatus().toString());
			setAndPublishSensedState(newState);
			switch(newState) {
			case COMPLETE:				
				localReset(); 
				doAutoResetAfterXseconds();
				break;
			case COMPLETING:
				break;
			case EXECUTE:
				break;
			case IDLE:
				checkIfAvailableForNextOrder();
				break;
			case RESETTING:
				localReset(); 
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
	
	private void localReset() {
		reservedForOrder = null;
	}
	
	private void reset() {
		localReset();
		hal.reset();
	}
	
	private void setAndPublishSensedState(BasicMachineStates newState) {
		String msg = String.format("%s sets state from %s to %s (Order: %s)", this.machineId.getId(), this.currentState, newState, lastOrder);
		log.debug(msg);
		this.currentState = newState;
		MachineUpdateEvent mue = new MachineStatusUpdateEvent(machineId.getId(), OPCUABasicMachineBrowsenames.STATE_VAR_NAME, msg, newState);
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
		if (currentState == BasicMachineStates.IDLE && !orders.isEmpty() && reservedForOrder == null) { // if we are idle, tell next order to get ready, this logic is also triggered upon machine signaling completion
			RegisterProcessStepRequest ror = orders.remove(0);
			lastOrder = ror.getRootOrderId();
			log.info("Ready for next Order: "+ror.getRootOrderId());
			reservedForOrder = ror; 
    		ror.getRequestor().tell(new ReadyForProcessEvent(ror), getSelf());
    	}	
	}	
	
	private void plotUponLockForOrder(LockForOrder lockReq) {
		if (currentState == BasicMachineStates.IDLE) {
    		// we need to extract from the reservedOrder the step, and from the step the input properties		        	
			if (reservedForOrder != null || reservedForOrder.getProcessStepId().equals(lockReq.getStepId())) {
				String imgName = "demo";
				try {
					imgName = extractInputFromProcessStep(reservedForOrder.getProcessStep());
				} catch (ProcessRequestException e) {					
					e.printStackTrace();
					// this should not happen as we check before and only stored the request when there was no exception
				}
				log.info("Requesting to plot: "+imgName);
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
    		sender().tell(new MachineInWrongStateResponse(this.machineId.getId(), OPCUABasicMachineBrowsenames.STATE_VAR_NAME, msg, currentState, lockReq, BasicMachineStates.IDLE), self);
    	}
	}
	
	private String extractInputFromProcessStep(ProcessStep p) throws ProcessRequestException {
		if (p == null) throw new ProcessRequestException(ProcessRequestException.Type.PROCESS_STEP_MISSING, "Provided Process Step is null");
		if (p instanceof CapabilityInvocation && ((CapabilityInvocation) p).getInvokedCapability() != null) {
			CapabilityInvocation ac = ((CapabilityInvocation) p);
			if (!(ac.getInvokedCapability().getUri().equals(cap.getUri()))) throw new ProcessRequestException(ProcessRequestException.Type.UNSUPPORTED_CAPABILITY, "Process Step Capability is not supported: "+ac.getInvokedCapability().getUri());
			EList<VariableMapping> inputs = ac.getInputMappings();			
			if (inputs != null) {
				Optional<Parameter> optP = inputs.stream()
						.filter(in -> in.getLhs().getName().equals(WellknownPlotterCapability.PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME) )
						.map(in -> in.getRhs())
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
		} else throw new ProcessRequestException(ProcessRequestException.Type.STEP_MISSES_CAPABILITY, "Process Step is not a capability invocation");
	}
	
	private void tellEventBus(MachineUpdateEvent mue) {
		externalHistory.add(mue);
		tellEventBusWithoutAddingToHistory(mue);
		lastMUE=mue;
	//	resendLastEvent();
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
            	if (currentState.equals(BasicMachineStates.COMPLETE) 
            			|| currentState.equals(BasicMachineStates.STOPPED) ) {	        		
            		setAndPublishSensedState(BasicMachineStates.RESETTING); // not sensed, but machine would do the same (or fail, then we need to wait for machine to respond)            		
            		reset();
            	} else if (currentState.equals(BasicMachineStates.COMPLETING) 
            			|| currentState.equals(BasicMachineStates.STOPPING) ) {
            		// we only recheck later of we are still in states leading to complete or stopped
            		doAutoResetAfterXseconds() ;          		
            	} else { // noop             		
            	}
            }
          }, context().system().dispatcher());
	}

}
