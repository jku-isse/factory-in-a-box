package fiab.mes.mockactors;

import java.time.Duration;
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
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;
import fiab.mes.restendpoint.requests.MachineHistoryRequest;


public class MockMachineActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected ActorSelection eventBusByRef;
	protected final AkkaActorBackedCoreModelAbstractActor machine;
	protected AbstractCapability cap;
	protected MachineStatus currentState;
	protected List<RegisterProcessStepRequest> orders = new ArrayList<>();
	protected RegisterProcessStepRequest reservedForOrder = null;
	private List<MachineEvent> history = new ArrayList<MachineEvent>();
	
	static public Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor) {	    
		return Props.create(MockMachineActor.class, () -> new MockMachineActor(machineEventBus, cap, modelActor));
	}
	
	public MockMachineActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor) {
		this.cap = cap;
		this.machine = new AkkaActorBackedCoreModelAbstractActor(modelActor.getID(), modelActor, self());
		this.eventBusByRef = machineEventBus;
		init();
		setAndPublishNewState(MachineStatus.IDLE);
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
		        		//TODO: here we assume correct invocation: thus on overtaking etc, will be improved later
		        		setAndPublishNewState(MachineStatus.EXECUTE); // we skip starting state here  
		        		finishProduction();
		        	} else {
		        		log.warning("Received lock for order in state: "+currentState);
		        	}
		        })
		        .match(MachineHistoryRequest.class, req -> {
		        	log.info(String.format("Machine %s received MachineHistoryRequest", machine.getId()));
		        	List<MachineEvent> events = req.shouldResponseIncludeDetails() ? history :	history.stream().map(event -> event.getCloneWithoutDetails()).collect(Collectors.toList());
		        	sender().tell(new MachineHistoryRequest.Response(machine.getId(), events, req.shouldResponseIncludeDetails()), getSelf());
		        })
		        .match(MachineEvent.class, e -> {
		        	history.add(e);
		        })
		        .build();
	}

	private void init() {
		eventBusByRef.tell(new MachineConnectedEvent(machine, Collections.singleton(cap), Collections.emptySet(), ""), self());
	}
	
	private void setAndPublishNewState(MachineStatus newState) {
		log.debug(String.format("%s sets state from %s to %s", this.machine.getId(), this.currentState, newState));
		this.currentState = newState;
		eventBusByRef.tell(new MachineStatusUpdateEvent(machine.getId(), null, WellknownMachinePropertyFields.STATE_VAR_NAME, "", newState), self());
	}
	
	private void checkIfAvailableForNextOrder() {
		log.debug(String.format("Checking if %s is IDLE: %s", this.machine.getId(), this.currentState));
		if (currentState == MachineStatus.IDLE && !orders.isEmpty() && reservedForOrder == null) { // if we are idle, tell next order to get ready, this logic is also triggered upon machine signaling completion
			RegisterProcessStepRequest ror = orders.remove(0);
			log.info("Ready for next Order: "+ror.getRootOrderId());
			reservedForOrder = ror; 
    		ror.getRequestor().tell(new ReadyForProcessEvent(ror), getSelf());
    	}	
	}	
	
	private void finishProduction() {
		log.debug("finishProduction in 5 seconds..");
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(5000), 
    			 new Runnable() {
            @Override
            public void run() {
            	log.debug("*****   make STOPPING "+machine.getId());
            	setAndPublishNewState(MachineStatus.COMPLETING); 
            	resetToIdle();
            }
          }, context().system().dispatcher());
	}
	
	private void resetToIdle() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	reservedForOrder = null;
            	setAndPublishNewState(MachineStatus.IDLE); // we then skip completed state
            	checkIfAvailableForNextOrder();
            }
          }, context().system().dispatcher());
	}
}
