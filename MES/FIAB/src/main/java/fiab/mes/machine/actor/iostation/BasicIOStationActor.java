package fiab.mes.machine.actor.iostation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import fiab.mes.machine.actor.iostation.wrapper.IOStationWrapperInterface;
import fiab.mes.machine.msg.IOStationStatusUpdateEvent;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.machine.msg.GenericMachineRequests.Reset;
import fiab.mes.machine.msg.GenericMachineRequests.Stop;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;
import fiab.mes.transport.handshake.HandshakeProtocol;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;

public class BasicIOStationActor extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected ActorSelection eventBusByRef;
	protected final AkkaActorBackedCoreModelAbstractActor machineId;
	protected AbstractCapability cap;
	protected HandshakeProtocol.ServerSide currentState = ServerSide.Unknown;
	protected IOStationWrapperInterface hal;
	protected InterMachineEventBus intraBus;
	protected boolean doAutoReset = true;
	protected boolean isInputStation = false;
	protected boolean isOutputStation = false;
	private ActorRef self;
	protected HistoryTracker externalHistory=null;
	
	protected List<RegisterProcessStepRequest> orders = new ArrayList<>();
	private String lastOrder;
	protected RegisterProcessStepRequest reservedForOrder = null;
	
	
	static public Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, IOStationWrapperInterface hal, InterMachineEventBus intraBus) {	    
		return Props.create(BasicIOStationActor.class, () -> new BasicIOStationActor(machineEventBus, cap, modelActor, hal, intraBus));
	}
	
	public BasicIOStationActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor, IOStationWrapperInterface hal, InterMachineEventBus intraBus) {
		this.cap = cap;
		this.machineId = new AkkaActorBackedCoreModelAbstractActor(modelActor.getID(), modelActor, self());
		this.eventBusByRef = machineEventBus;
		this.hal = hal;
		this.intraBus = intraBus;
		this.externalHistory = new HistoryTracker(machineId.getId());
		this.self = self();
		init();
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		.match(RegisterProcessStepRequest.class, registerReq -> {
        	orders.add(registerReq);
        	log.info(String.format("Order %s registered.", registerReq.getRootOrderId()));
        	if ((currentState == ServerSide.IdleEmpty && isOutputStation) || 
        			(currentState == ServerSide.IdleLoaded && isInputStation)) {
        		triggerNextQueuedOrder();
        	}
        } )
        .match(LockForOrder.class, lockReq -> {
        	log.info("received LockForOrder msg "+lockReq.getStepId()+", current state: "+currentState);
        	if ((currentState == ServerSide.IdleEmpty && isOutputStation) || 
        			(currentState == ServerSide.IdleLoaded && isInputStation)) {
        		// we are still in the right state, now we provide/receive the reserved order
        		// nothing to be done here
        	} else {
        		log.warning("Received lock for order in state: "+currentState);
        	}
        })
        .match(IOStationStatusUpdateEvent.class, mue -> {
        	processIOStationStatusUpdateEvent(mue);
        })
        .match(Stop.class, req -> {
        	log.info(String.format("IOStation %s received StopRequest", machineId.getId()));
        	setAndPublishSensedState(ServerSide.Stopping);
        	hal.stop();
        })
        .match(Reset.class, req -> {
        	if (currentState.equals(ServerSide.Completed) 
        			|| currentState.equals(ServerSide.Stopped) ) {
        		log.info(String.format("IOStation %s received ResetRequest in suitable state", machineId.getId()));
        		setAndPublishSensedState(ServerSide.Resetting); // not sensed, but machine would do the same (or fail, then we need to wait for machine to respond)
        		hal.reset();
        	} else {
        		log.warning(String.format("IOStation %s received ResetRequest in non-COMPLETE or non-STOPPED state, ignoring", machineId.getId()));
        	}
        })
        .match(MachineHistoryRequest.class, req -> {
        	log.info(String.format("Machine %s received MachineHistoryRequest", machineId.getId()));
        	externalHistory.sendHistoryResponseTo(req, getSender(), self);
        })
        .build();
	}

	private void init() {
		if (this.cap.equals(HandshakeProtocol.getInputStationCapability())) {
			isInputStation = true;
		}
		if (this.cap.equals(HandshakeProtocol.getOutputStationCapability())) {
			isOutputStation = true;
		}
		eventBusByRef.tell(new MachineConnectedEvent(machineId, Collections.singleton(cap), Collections.emptySet()), self());
		intraBus.subscribe(getSelf(), new SubscriptionClassifier(machineId.getId(), "*")); //ensure we get all events on this bus, but never our own, should we happen to accidentally publish some
		hal.subscribeToStatus();
		hal.subscribeToLoadStatus();
	}
	
	private void setAndPublishSensedState(ServerSide newState) {
		String msg = String.format("%s sets state from %s to %s (Order: %s)", this.machineId.getId(), this.currentState, newState, lastOrder);
		log.info(msg);
		this.currentState = newState;
		MachineUpdateEvent mue = new IOStationStatusUpdateEvent(machineId.getId(), msg, newState);
		externalHistory.add(mue);
		tellEventBus(mue);
	}
	
	private void tellEventBus(MachineUpdateEvent mue) {
		//externalHistory.add(mue);
		eventBusByRef.tell(mue, self());
	}
	
	private void processIOStationStatusUpdateEvent(IOStationStatusUpdateEvent mue) {
		if (mue.getParameterName().equals(HandshakeProtocol.STATE_SERVERSIDE_VAR_NAME)) {
			ServerSide newState = mue.getStatus();
			setAndPublishSensedState(newState);
			switch(newState) {
			case IdleEmpty:
				if (isOutputStation) { // ready to receive pallet as an outputstation
					triggerNextQueuedOrder();
		    	}
				break;
			case IdleLoaded:
				if (isInputStation) { // ready to provide pallet as an inputstation
					triggerNextQueuedOrder();
		    	}	
				break;
			case Stopped:
				if (doAutoReset)
					reset();
				break;
			case Completed:
				reset(); //we automatically reset, might be done also by station itself, but we need to clean state here as well
				break;
			default:
				break;
			
			}
		}
	}
	
	private void triggerNextQueuedOrder() {
		if ( !orders.isEmpty() && reservedForOrder == null) { 
			RegisterProcessStepRequest ror = orders.remove(0);
			lastOrder = ror.getRootOrderId();
			log.info("Ready for next Order: "+ror.getRootOrderId());
			reservedForOrder = ror; 
    		ror.getRequestor().tell(new ReadyForProcessEvent(ror), getSelf());
    	}	
	}	
	
	private void reset() {
		reservedForOrder = null;
		lastOrder = null;
		hal.reset();
	}
	
}
