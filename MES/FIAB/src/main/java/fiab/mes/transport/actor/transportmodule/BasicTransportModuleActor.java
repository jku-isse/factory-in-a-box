package fiab.mes.transport.actor.transportmodule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.sun.xml.bind.v2.WellKnownNamespace;

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
import fiab.mes.machine.msg.GenericMachineRequests.BaseRequest;
import fiab.mes.machine.msg.GenericMachineRequests.Reset;
import fiab.mes.machine.msg.GenericMachineRequests.Stop;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineInWrongStateResponse;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;
import fiab.mes.transport.actor.transportmodule.wrapper.TransportModuleWrapperInterface;
import fiab.mes.transport.actor.transportsystem.TransportPositionLookup;
import fiab.mes.transport.actor.transportsystem.TransportRoutingInterface.Position;
import fiab.mes.transport.msg.InternalTransportModuleRequest;
import fiab.mes.transport.msg.TransportModuleRequest;


public class BasicTransportModuleActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected ActorSelection eventBusByRef;
	protected final AkkaActorBackedCoreModelAbstractActor machineId;
	protected AbstractCapability cap;
	protected MachineStatus currentState = MachineStatus.UNKNOWN;
	protected TransportModuleWrapperInterface hal;
	protected InterMachineEventBus intraBus;
	protected TransportPositionLookup tpl;
	protected Position selfPos;
	protected InternalCapabilityToPositionMapping icpm;
	protected InternalTransportModuleRequest reservedForTReq = null;
	private ActorRef self;
	
	private HistoryTracker externalHistory;
	//private List<MachineEvent> internalHistory = new ArrayList<MachineEvent>();
	
	static public Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, TransportModuleWrapperInterface hal, Position selfPos, InterMachineEventBus intraBus, TransportPositionLookup tpl, InternalCapabilityToPositionMapping icpm) {	    
		return Props.create(BasicTransportModuleActor.class, () -> new BasicTransportModuleActor(machineEventBus, cap, modelActor, hal, selfPos, intraBus, tpl, icpm));
	}
	
	public BasicTransportModuleActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, TransportModuleWrapperInterface hal, Position selfPos, InterMachineEventBus intraBus, TransportPositionLookup tpl, InternalCapabilityToPositionMapping icpm) {
		this.cap = cap;
		this.machineId = new AkkaActorBackedCoreModelAbstractActor(modelActor.getID(), modelActor, self());
		this.eventBusByRef = machineEventBus;
		this.hal = hal;
		this.intraBus = intraBus;
		this.tpl = tpl; // later versions will obtain such info dynamically from accessing own capability OPC UA information
		this.icpm = icpm; // later versions will obtain such info dynamically from accessing own wiring OPC UA information
		this.selfPos = selfPos;
		this.self = self();
		this.externalHistory = new HistoryTracker(machineId.getId());
		init();

	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// map from positions to capabilityInstances local to the transport module 
		        .match(TransportModuleRequest.class, req -> {
		        	log.info(String.format("Received TransportModuleRequest from %s to %s for order %s", req.getPosFrom(), req.getPosTo(), req.getOrderId()));
		        	if (currentState.equals(MachineStatus.IDLE)) {
		        		processTransportModuleRequest(req);
		        	} else {
		        		String msg = String.format("Received TransportModuleRequest %s in incompatible local state %s", req.getOrderId(), this.currentState);
		        		log.warning(msg);
		        		getSender().tell(new MachineInWrongStateResponse(machineId.getId(), WellknownMachinePropertyFields.STATE_VAR_NAME, msg, this.currentState, req, MachineStatus.IDLE), self());
		        	}
		        })
		        .match(MachineStatusUpdateEvent.class, mue -> {
		        	processMachineUpdateEvent(mue);
		        })
		        .match(MachineHistoryRequest.class, req -> {
		        	log.info(String.format("Machine %s received MachineHistoryRequest", machineId.getId()));
		        	externalHistory.sendHistoryResponseTo(req, getSender(), self);
		        })
		        .match(Stop.class, req -> {
		        	log.info(String.format("TransportModule %s received StopRequest", machineId.getId()));
		        	setAndPublishSensedState(MachineStatus.STOPPING);
		        	hal.stop();
		        })
		        .match(Reset.class, req -> {
		        	if (currentState.equals(MachineStatus.COMPLETE) 
		        			|| currentState.equals(MachineStatus.STOPPED) ) {
		        		log.info(String.format("TransportModule %s received ResetRequest in suitable state", machineId.getId()));
		        		setAndPublishSensedState(MachineStatus.RESETTING); // not sensed, but machine would do the same (or fail, then we need to wait for machine to respond)
		        		hal.reset();
		        	} else {
		        		log.warning(String.format("TransportModule %s received ResetRequest in non-COMPLETE or non-STOPPED state, ignoring", machineId.getId()));
		        	}
		        })
		        .build();
	}

	private void init() {
		eventBusByRef.tell(new MachineConnectedEvent(machineId, Collections.singleton(cap), Collections.emptySet()), self());
		intraBus.subscribe(getSelf(), new SubscriptionClassifier(machineId.getId(), "*")); //ensure we get all events on this bus, but never our own, should we happen to accidentally publish some
		hal.subscribeToStatus();
	}
	
	private void processTransportModuleRequest(TransportModuleRequest req) {
		//we check if the requests directions encoded in the capabilities are indeed found on this transportmodule
		Optional<String> capFrom = icpm.getCapabilityIdForPosition(req.getPosFrom(), selfPos);
		Optional<String> capTo = icpm.getCapabilityIdForPosition(req.getPosTo(), selfPos);
		if (capFrom.isPresent() && capTo.isPresent()) {
			setAndPublishSensedState(MachineStatus.STARTING);
			reservedForTReq = new InternalTransportModuleRequest(capFrom.get(), capTo.get(), req.getOrderId(), req.getRequestId());
			hal.transport(reservedForTReq);
		} else {
			log.warning(String.format("TransportModuleRequest %s from %s to %s cannt be resolved to local capabilities", req.getOrderId(), req.getPosFrom(), req.getPosTo()));
			//TODO: return error message to sender
		}
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
			case EXECUTE: // for now we guess to have obtained the pallet for given order, --> this would need to be confirmed by the sub actor representing the turntable
				break;
			case IDLE:
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
		String order = reservedForTReq != null ? reservedForTReq.getOrderId() : "none";
		String msg = String.format("%s sets state from %s to %s (Order: %s)", this.machineId.getId(), this.currentState, newState, order);
		log.debug(msg);
		if (currentState != newState) {
			this.currentState = newState;
			MachineUpdateEvent mue = new MachineStatusUpdateEvent(machineId.getId(), null, WellknownMachinePropertyFields.STATE_VAR_NAME, msg, newState);
			tellEventBus(mue);
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