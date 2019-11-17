package fiab.mes.mockactors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;


public class MockInputStationActor extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected ActorSelection eventBusByRef;
	protected final AkkaActorBackedCoreModelAbstractActor machineId;
	protected AbstractCapability cap;
	protected String currentState;
	
	protected List<RegisterProcessStepRequest> orders = new ArrayList<>();
	
	static public Props props(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor) {	    
		return Props.create(MockInputStationActor.class, () -> new MockInputStationActor(machineEventBus, cap, modelActor));
	}
	
	public MockInputStationActor(ActorSelection machineEventBus, AbstractCapability cap, Actor modelActor) {
		this.cap = cap;
		this.machineId = new AkkaActorBackedCoreModelAbstractActor(modelActor.getID(), modelActor, self());
		this.eventBusByRef = machineEventBus;
		init();
		setAndPublishNewState(MachineOrderMappingManager.IDLE_STATE_VALUE);
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
		        	if (currentState == MachineOrderMappingManager.IDLE_STATE_VALUE) {
		        		//TODO: here we assume correct invocation: thus on overtaking etc, will be improved later
		        		setAndPublishNewState(MachineOrderMappingManager.PRODUCING_STATE_VALUE); // we skip starting state here  
		        		finishProduction();
		        	} else {
		        		log.warning("Received lock for order in state: "+currentState);
		        	}
		        })
		        .build();
	}

	private void init() {
		eventBusByRef.tell(new MachineConnectedEvent(machineId, Collections.singleton(cap), Collections.emptySet()), self());
	}
	
	private void setAndPublishNewState(String newState) {
		this.currentState = newState;
		eventBusByRef.tell(new MachineUpdateEvent(machineId.getId(), null, MachineOrderMappingManager.STATE_VAR_NAME, newState), self());
	}
	
	private void checkIfAvailableForNextOrder() {
		if (currentState == MachineOrderMappingManager.IDLE_STATE_VALUE && !orders.isEmpty()) { // if we are idle, tell next order to get ready, this logic is also triggered upon machine signaling completion
			RegisterProcessStepRequest ror = orders.remove(0);
			log.info("Ready for next Order: "+ror.getRootOrderId());
    		ror.getRequestor().tell(new ReadyForProcessEvent(ror), getSelf());
    	}	
	}	
	
	private void finishProduction() {
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(5000), 
    			 new Runnable() {
            @Override
            public void run() {
            	setAndPublishNewState(MachineOrderMappingManager.COMPLETING_STATE_VALUE); 
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
            	setAndPublishNewState(MachineOrderMappingManager.IDLE_STATE_VALUE); // we then skip completed state
            	checkIfAvailableForNextOrder();
            }
          }, context().system().dispatcher());
	}
}
