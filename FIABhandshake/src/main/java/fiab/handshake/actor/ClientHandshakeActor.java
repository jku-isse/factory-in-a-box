package fiab.handshake.actor;

import java.time.Duration;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import brave.Span;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.handshake.actor.messages.HSClientMessage;
import fiab.handshake.actor.messages.HSServerMessage;
import fiab.tracing.actor.AbstractTracingActor;
import fiab.core.capabilities.handshake.HandshakeCapability.ClientSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;

public class ClientHandshakeActor extends AbstractTracingActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private ActorRef machineWrapper;
	private ActorRef serverSide;
	private ClientSideStates currentState = ClientSideStates.STOPPED;
	private ServerSideStates remoteState = null;
	private ActorRef self;

	private StatePublisher publishEP;

	static public Props props(ActorRef machineWrapper, ActorRef serverSide) {
		return Props.create(ClientHandshakeActor.class, () -> new ClientHandshakeActor(machineWrapper, serverSide));
	}

	static public Props props(ActorRef machineWrapper, ActorRef serverSide, StatePublisher publishEP) {
		return Props.create(ClientHandshakeActor.class,
				() -> new ClientHandshakeActor(machineWrapper, serverSide, publishEP));
	}

	public ClientHandshakeActor(ActorRef machineWrapper, ActorRef serverSide) {
		this.machineWrapper = machineWrapper;
		this.serverSide = serverSide;
		this.self = getSelf();
	}

	public ClientHandshakeActor(ActorRef machineWrapper, ActorRef serverSide, StatePublisher publishEP) {
		this.machineWrapper = machineWrapper;
		this.serverSide = serverSide;
		this.publishEP = publishEP;
		this.self = getSelf();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(IOStationCapability.ClientMessageTypes.class, msg -> { // commands from parent FU
			//
			receiveClientMessage(new HSClientMessage("", msg));

		}).match(HSClientMessage.class, msg -> { // command from parent FU with Tracing Info
			receiveClientMessage(msg);

		}).match(IOStationCapability.ServerMessageTypes.class, resp -> { // responses to requests

		}).match(HSServerMessage.class, rsp -> { // response to requests with tracing
			receiveServerMessage(rsp);

		}).match(ServerSideStates.class, msg -> { // state event updates
			log.info(String.format("Received %s from %s in local state %s", msg, getSender(), currentState));
			if (getSender().equals(serverSide)) {
				remoteState = msg;
				switch (msg) {
				case IDLE_EMPTY: // fallthrough
				case IDLE_LOADED:
					if (currentState.equals(ClientSideStates.STARTING)
							|| currentState.equals(ClientSideStates.INITIATING))
						requestInitiateHandover();
					break;
				case COMPLETE: // fallthrough, if serverside is done, we can do the same
				case COMPLETING:
					// onlfy if in executing
					if (currentState.equals(ClientSideStates.EXECUTE))
						complete();
					break;
				case EXECUTE: // if server side is executing, we can do the same if we are in ready
					if (currentState.equals(ClientSideStates.READY))
						receiveStartOkResponse();
					break;
				case READY_EMPTY: // fallthrough
				case READY_LOADED:
					if (currentState.equals(ClientSideStates.READY)) {
						requestStartHandover();
					}
					break;
				case STOPPED: // fallthrough
				case STOPPING: // if we are in any state that would not expect a stop, then we also need to
								// stop/abort
					if (// currentState.equals(ClientSide.Initiating) ||
						// currentState.equals(ClientSide.Initiated) || //the server might not be ready
						// yet, thus we need to wait, not stop
					currentState.equals(ClientSideStates.READY) || currentState.equals(ClientSideStates.EXECUTE))
						stop();
					break;
				default:
					break;
				}
			} else {
				log.warning(String.format("Received %s to unexpected sender %s", msg, getSender()));
			}
		}).matchAny(msg -> {
			log.warning("Unexpected Message received: " + msg.toString());
		}).build();
	}
	
	private void receiveClientMessage(HSClientMessage msg) {

		IOStationCapability.ClientMessageTypes body = msg.getBody();
		switch (body) {
		case Reset:
			reset(); // prepare for next round
			break;
		case Start:
			Span span = getTracingFactory().createNewTrace("Handshake", "Start");
			start(""); // engage in handshake: subscribe to state updates
			span.start();
			break;
		case Complete:
			if (currentState.equals(ClientSideStates.EXECUTE)) // only if we are in state executing, otherwise complete
																// makes no sense
				complete(); // handshake can be wrapped up
			break;
		case Stop:
			stop(); // error or external stop, otherwise autostopping upon completion
			break;
		default:
			break;
		}
	}

	private void receiveServerMessage(HSServerMessage msg) {
		IOStationCapability.ServerMessageTypes resp = msg.getBody();

		switch (resp) {
		case NotOkResponseInitHandover:
			stop();
			break;
		case NotOkResponseStartHandover:
			stop();
			break;
		case OkResponseInitHandover:
			receiveInitiateOkResponse();
			break;
		case OkResponseStartHandover:
			receiveStartOkResponse();
			break;
		default:
			log.warning("Unexpected ServerSide MessageType received: " + resp.toString());
			break;
		}

	}

	

	private void publishNewState(ClientSideStates newState) {
		currentState = newState;
		machineWrapper.tell(newState, getSelf());
		if (publishEP != null)
			publishEP.setStatusValue(newState.toString());
	}

	private void reset() {
		publishNewState(ClientSideStates.RESETTING);
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				transitionResettingToIdle();
			}
		}, context().system().dispatcher());
	}

	private void transitionResettingToIdle() {
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				publishNewState(ClientSideStates.IDLE);
			}
		}, context().system().dispatcher());
	}

	private void start(String header) {
		if (currentState.equals(ClientSideStates.IDLE)) {
			publishNewState(ClientSideStates.STARTING);
			serverSide.tell(IOStationCapability.ServerMessageTypes.SubscribeToStateUpdates, getSelf()); // subscribe for
																										// updates
			publishNewState(ClientSideStates.INITIATING);
		} else {
			log.warning("was requested invalid command 'Start' in state: " + currentState);
		}
	}

	private void requestInitiateHandover() {
		getSender().tell(IOStationCapability.ServerMessageTypes.RequestInitiateHandover, self);
		retryInit();
	}

	private void retryInit() {
		context().system().scheduler().scheduleOnce(Duration.ofMillis(5000), new Runnable() {
			@Override
			public void run() {
				// if still waiting:
				if (currentState.equals(ClientSideStates.INITIATING)) {
					log.info("Retrying to send InitiateHandover");
					requestInitiateHandover();
				}
			}
		}, context().system().dispatcher());
	}

	private void receiveInitiateOkResponse() {
		publishNewState(ClientSideStates.INITIATED);
		context().system().scheduler().scheduleOnce(Duration.ofMillis(2000), new Runnable() {
			@Override
			public void run() {
				publishNewState(ClientSideStates.READY);
				if (remoteState.equals(ServerSideStates.READY_EMPTY)
						|| remoteState.equals(ServerSideStates.READY_LOADED)) {
					// only of remote point is ready, and we are also still ready (as this is
					// triggered some time in the future)
					if (currentState.equals(ClientSideStates.READY))
						requestStartHandover();
				} else {
					log.info(String.format("Server %s in last known state %s not yet ready for RequestStartHandover",
							serverSide, remoteState));
				}
			}
		}, context().system().dispatcher());

	}

	private void requestStartHandover() {
		if (currentState.equals(ClientSideStates.READY)) {
			log.info(String.format("Requesting StartHandover from remote %s", serverSide));
			serverSide.tell(IOStationCapability.ServerMessageTypes.RequestStartHandover, self);
			retryStartHandover();
		} else {
			log.warning("was requested invalid command 'StartHandover' in state: " + currentState);
		}
	}

	private void retryStartHandover() {
		context().system().scheduler().scheduleOnce(Duration.ofMillis(3000), new Runnable() {
			@Override
			public void run() {
				// if still Ready:
				if (currentState.equals(ClientSideStates.READY)) {
					log.info("Retrying to send StartHandover");
					requestStartHandover();
				}
			}
		}, context().system().dispatcher());
	}

	private void receiveStartOkResponse() {
		publishNewState(ClientSideStates.EXECUTE);
	}

	private void complete() {
		publishNewState(ClientSideStates.COMPLETING);
		if (serverSide != null) {
			serverSide.tell(IOStationCapability.ServerMessageTypes.UnsubscribeToStateUpdates, self);
		}
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				publishNewState(ClientSideStates.COMPLETED);
			}
		}, context().system().dispatcher());
	}

	private void stop() {
		publishNewState(ClientSideStates.STOPPING);
		if (serverSide != null) {
			serverSide.tell(IOStationCapability.ServerMessageTypes.UnsubscribeToStateUpdates, self);
		}
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				publishNewState(ClientSideStates.STOPPED);
			}
		}, context().system().dispatcher());
	}
}
