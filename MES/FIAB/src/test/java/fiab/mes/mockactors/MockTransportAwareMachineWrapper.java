package fiab.mes.mockactors;

import java.time.Duration;
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
import akka.testkit.CallingThreadDispatcher;
import fiab.mes.eventbus.InterMachineEventBus;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.actor.WellknownMachinePropertyFields;
import fiab.mes.machine.msg.MachineConnectedEvent;
import fiab.mes.machine.msg.MachineEvent;
import fiab.mes.machine.msg.MachineStatus;
import fiab.mes.machine.msg.MachineStatusUpdateEvent;
import fiab.mes.machine.msg.MachineUpdateEvent;
import fiab.mes.mockactors.MockMachineWrapper.MessageTypes;
import fiab.mes.order.msg.LockForOrder;
import fiab.mes.order.msg.ReadyForProcessEvent;
import fiab.mes.order.msg.RegisterProcessStepRequest;
import fiab.mes.planer.actor.MachineOrderMappingManager;
import fiab.mes.transport.handshake.HandshakeProtocol.ClientSide;
import fiab.mes.transport.handshake.HandshakeProtocol.ServerSide;


public class MockTransportAwareMachineWrapper extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected InterMachineEventBus interEventBus;
	protected MachineStatus currentState = MachineStatus.STOPPED;
	protected boolean doPublishState = false;
	protected ServerSide handshakeStatus;
	protected ActorRef serverSide;
	protected ActorRef self;
	
	static public Props props(InterMachineEventBus internalMachineEventBus) {	    
		return Props.create(MockTransportAwareMachineWrapper.class, () -> new MockTransportAwareMachineWrapper(internalMachineEventBus));
	}
	
	public MockTransportAwareMachineWrapper(InterMachineEventBus machineEventBus) {
		this.interEventBus = machineEventBus;
		// setup serverhandshake actor with autocomplete
		boolean doAutoComplete = true;
		self = getSelf();
		//serverSide = getContext().actorOf(MockServerHandshakeActor.props(getSelf(), doAutoComplete).withDispatcher(CallingThreadDispatcher.Id()), "ServerSideHandshakeMock");
		serverSide = getContext().actorOf(MockServerHandshakeActor.props(getSelf(), doAutoComplete), "ServerSideHandshakeMock");		
	}
	
	public Receive createReceive() {
		return receiveBuilder()
				.match(MessageTypes.class, msg -> {
					switch(msg) {
					case SubscribeState: 
						doPublishState = true;
						setAndPublishState(currentState); //we publish the current state
						break;
					case Plot:
						if (currentState.equals(MachineStatus.IDLE))
							plot();
						else 
							log.warning("Wrapper told to plot in wrong state "+currentState);
						break;
					case Reset:
						if (currentState.equals(MachineStatus.STOPPED))
							reset();
						else 
							log.warning("Wrapper told to reset in wrong state "+currentState);
						break;
					case Stop:
						stop();
						break;
					default:
						break;
					}
				})
				.match(ServerSide.class, msg -> { // state event updates
					log.info(String.format("Received %s from %s", msg, getSender()));
					if (getSender().equals(serverSide)) {
						handshakeStatus = msg;
						switch(msg) {
						case Completed: // handshake complete, thus un/loading done
							if  (currentState.equals(MachineStatus.STARTING) ) { // pallet is now loaded
								transitionStartingToExecute();
							} else if (currentState.equals(MachineStatus.COMPLETING)) { // pallet is now unloaded
								transitionCompletingToComplete();
							}
							break;
						case Stopped: 
							if (currentState.equals(MachineStatus.STOPPING) ) //only if we wait for FU to stop
								transitionToStop();
							break;
						default: // irrelevant states
							break;
						}
					} else {
						log.warning(String.format("Received %s to unexpected sender %s", msg, getSender()));
					}
				})
				.matchAny(msg -> { 
					log.warning("Unexpected Message received: "+msg.toString()); })
		        .build();
	}
	
	private void setAndPublishState(MachineStatus newState) {
		//log.debug(String.format("%s sets state from %s to %s", this.machineId.getId(), this.currentState, newState));
		this.currentState = newState;
		if (doPublishState) {
			interEventBus.publish(new MachineStatusUpdateEvent("", null, WellknownMachinePropertyFields.STATE_VAR_NAME, "", newState));
		}
	}

	private void reset() {
		setAndPublishState(MachineStatus.RESETTING);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// only when plottingFU is in idle, but we dont care about transport/handshake FU, which stays in stopped as the order for which we load is not clear yet
            	setAndPublishState(MachineStatus.IDLE); 
            }
          }, context().system().dispatcher());
	}
	
	private void stop() {
		setAndPublishState(MachineStatus.STOPPING);
		serverSide.tell(MockServerHandshakeActor.MessageTypes.Stop, getSelf());
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(1000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// only when handshakeFU and other FUs have stopped
            	if (handshakeStatus.equals(ServerSide.Stopped)) {
            		transitionToStop();
            	}
            }
          }, context().system().dispatcher());
	}
	
	private void transitionToStop() {
		setAndPublishState(MachineStatus.STOPPED); 
	}
	
	private void plot() {
		setAndPublishState(MachineStatus.STARTING);
		//now here we also enable pallet to be loaded onto machine
		serverSide.tell(MockServerHandshakeActor.MessageTypes.Reset, self);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(5000), 
    			 new Runnable() {
            @Override
            public void run() {
            	// we only transition when the pallet is loaded, e.g., the server handshake is completing or completed,
            	//sending of the complete() command (by the here nonexisting converyerFU when loaded) --> not necessary if we set serverside protocol actor to auto-complete
            	if (handshakeStatus.equals(ServerSide.Completed)) {
            		transitionStartingToExecute();
            	}
            }
          }, context().system().dispatcher());
	} 
	
	private void transitionStartingToExecute() {
		setAndPublishState(MachineStatus.EXECUTE);
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(5000), 
    			 new Runnable() {
            @Override
            public void run() {
            	finishProduction();
            }
          }, context().system().dispatcher());
	}
	
	private void finishProduction() {
		setAndPublishState(MachineStatus.COMPLETING); 
		serverSide.tell(MockServerHandshakeActor.MessageTypes.Reset, self); //now again do a handshake and unload,
		context().system()
    	.scheduler()
    	.scheduleOnce(Duration.ofMillis(3000), 
    			 new Runnable() {
            @Override
            public void run() {
            	if (handshakeStatus.equals(ServerSide.Completed)) {
            		//only when the handshake is in completed are we good to continue, actually we only care about loadstate
            		transitionCompletingToComplete();
            	}
            }
          }, context().system().dispatcher());
	}
	
	private void transitionCompletingToComplete() {
    	setAndPublishState(MachineStatus.COMPLETE); 
    	reset(); // we automatically reset
	}
	
}