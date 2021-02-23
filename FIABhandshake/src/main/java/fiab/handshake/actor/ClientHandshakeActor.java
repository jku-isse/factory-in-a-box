package fiab.handshake.actor;

import java.time.Duration;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.handshake.actor.messages.HSClientMessage;
import fiab.handshake.actor.messages.HSClientStateMessage;
import fiab.handshake.actor.messages.HSServerMessage;
import fiab.handshake.actor.messages.HSServerSideStateMessage;
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
		return receiveBuilder().match(IOStationCapability.ClientMessageTypes.class, rsp -> { // commands from parent FU
			receiveClientMessage(new HSClientMessage("", rsp));

		}).match(HSClientMessage.class, msg -> { // command from parent FU with tracing info
			receiveClientMessage(msg);

		}).match(IOStationCapability.ServerMessageTypes.class, rsp -> { // responses to requests
			receiveServerMessage(new HSServerMessage("", rsp));

		}).match(HSServerMessage.class, msg -> { // response to requests with tracing info
			receiveServerMessage(msg);

		}).match(ServerSideStates.class, rsp -> { // state event updates
			receiveServerSideState(new HSServerSideStateMessage("", rsp));

		}).match(HSServerSideStateMessage.class, msg -> {// state event updates with tracing info
			receiveServerSideState(msg);

		}).matchAny(msg -> {
			log.warning("Unexpected Message received: " + msg.toString());
		}).build();
	}

	private void receiveClientMessage(HSClientMessage msg) {
		IOStationCapability.ClientMessageTypes body = msg.getBody();

		log.info(String.format("Received %s from %s", body, getSender()));

		switch (body) {
		case Reset:
			try {
				tracingFactory.startConsumerSpan(msg, "Client-Handshake: Reset Received");
				reset(); // prepare for next round

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracingFactory.finishCurrentSpan();
			}

			break;
		case Start:
			try {
				tracingFactory.startConsumerSpan(msg, "Client-Handshake: Start Received");
				start(); // engage in handshake: subscribe to state updates
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracingFactory.finishCurrentSpan();
			}
			break;
		case Complete:
			if (currentState.equals(ClientSideStates.EXECUTE)) // only if we are in state executing, otherwise complete
																// makes no sense
				try {
					tracingFactory.startConsumerSpan(msg, "Client-Handshake: Complete Received");
					complete(); // handshake can be wrapped up
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					tracingFactory.finishCurrentSpan();
				}
			break;
		case Stop:
			try {
				tracingFactory.startConsumerSpan(msg, "Client-Handshake: Stop Received");
				stop(); // error or external stop, otherwise autostopping upon completion
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracingFactory.finishCurrentSpan();
			}

			break;
		default:
			break;
		}
	}

	private void receiveServerMessage(HSServerMessage msg) {
		IOStationCapability.ServerMessageTypes body = msg.getBody();
		log.info(String.format("Received %s from %s", body, getSender()));

		switch (body) {
		case NotOkResponseInitHandover:
			try {
				tracingFactory.startConsumerSpan(msg, "Client-Handshake: Not Ok Response Init Handover received");
				stop();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracingFactory.finishCurrentSpan();
			}
			break;
		case NotOkResponseStartHandover:
			try {
				tracingFactory.startConsumerSpan(msg, "Client-Handshake: Not Ok Response Start Handover received");
				stop();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracingFactory.finishCurrentSpan();
			}
			break;
		case OkResponseInitHandover:
			try {
				tracingFactory.startConsumerSpan(msg, "Client-Handshake: Ok Response Init Handover received");
				receiveInitiateOkResponse();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracingFactory.finishCurrentSpan();
			}
			break;
		case OkResponseStartHandover:
			try {
				tracingFactory.startConsumerSpan(msg, "Client-Handshake: Ok Response Start Handover received");
				receiveStartOkResponse();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracingFactory.finishCurrentSpan();
			}
			break;
		default:
			log.warning("Unexpected ServerSide MessageType received: " + body.toString());
			break;
		}

	}

	private void receiveServerSideState(HSServerSideStateMessage msg) {
		ServerSideStates body = msg.getBody();

		log.info(String.format("Received %s from %s in local state %s", body, getSender(), currentState));
		if (getSender().equals(serverSide)) {
			remoteState = body;
			switch (body) {
			case IDLE_EMPTY: // fallthrough
			case IDLE_LOADED:
				if (currentState.equals(ClientSideStates.STARTING) || currentState.equals(ClientSideStates.INITIATING))
					try {
						tracingFactory.startConsumerSpan(msg,
								"Client-Handshake: Server Side State: Idle Empty/Loaded received");
						requestInitiateHandover();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						tracingFactory.finishCurrentSpan();
					}

				break;
			case COMPLETE: // fallthrough, if serverside is done, we can do the same
			case COMPLETING:
				// onlfy if in executing
				if (currentState.equals(ClientSideStates.EXECUTE))
					try {
						tracingFactory.startConsumerSpan(msg,
								"Client-Handshake: Server Side State: Complete/Completing received");
						complete();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						tracingFactory.finishCurrentSpan();
					}
				break;
			case EXECUTE: // if server side is executing, we can do the same if we are in ready
				if (currentState.equals(ClientSideStates.READY)) {
					try {
						tracingFactory.startConsumerSpan(msg, "Client-Handshake: Server Side State: Execute received");
						receiveStartOkResponse();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						tracingFactory.finishCurrentSpan();
					}
				}
				break;
			case READY_EMPTY: // fallthrough
			case READY_LOADED:
				if (currentState.equals(ClientSideStates.READY)) {
					try {
						tracingFactory.startConsumerSpan(msg,
								"Client-Handshake: Server Side State: Ready Empty/Loaded received");
						requestStartHandover();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						tracingFactory.finishCurrentSpan();
					}
				}
				break;
			case STOPPED: // fallthrough
			case STOPPING: // if we are in any state that would not expect a stop, then we also need to
							// stop/abort
				if (// currentState.equals(ClientSide.Initiating) ||
					// currentState.equals(ClientSide.Initiated) || //the server might not be ready
					// yet, thus we need to wait, not stop
				currentState.equals(ClientSideStates.READY) || currentState.equals(ClientSideStates.EXECUTE)) {
					try {
						tracingFactory.startConsumerSpan(msg,
								"Client-Handshake: Server Side State: Stopped/Stopping received");
						stop();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						tracingFactory.finishCurrentSpan();
					}

				}
				break;
			default:
				break;
			}
		} else {
			log.warning(String.format("Received %s to unexpected sender %s", body, getSender()));
		}

	}

	private void publishNewState(ClientSideStates newState) {
		currentState = newState;

		HSClientStateMessage msg = new HSClientStateMessage(tracingFactory.getCurrentHeader(), newState);
		tracingFactory.injectMsg(msg);

		// TODO remove when all actors support extensible messages
		//machineWrapper.tell(newState, getSelf());

		machineWrapper.tell(msg, getSelf());
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

	private void start() {
		if (currentState.equals(ClientSideStates.IDLE)) {
			publishNewState(ClientSideStates.STARTING);

			HSServerMessage msg = new HSServerMessage(tracingFactory.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.SubscribeToStateUpdates);
			tracingFactory.injectMsg(msg);// subscribe for
			// updates

			serverSide.tell(msg, getSelf());
			publishNewState(ClientSideStates.INITIATING);

		} else {
			log.warning("was requested invalid command 'Start' in state: " + currentState);
		}
	}

	private void requestInitiateHandover() {
		HSServerMessage msg = new HSServerMessage(tracingFactory.getCurrentHeader(),
				IOStationCapability.ServerMessageTypes.RequestInitiateHandover);
		tracingFactory.injectMsg(msg);
		getSender().tell(msg, self);
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

			HSServerMessage msg = new HSServerMessage(tracingFactory.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.RequestStartHandover);
			tracingFactory.injectMsg(msg);
			serverSide.tell(msg, self);

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
			HSServerMessage msg = new HSServerMessage(tracingFactory.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.UnsubscribeToStateUpdates);
			tracingFactory.injectMsg(msg);
			serverSide.tell(msg, self);
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
			HSServerMessage msg = new HSServerMessage(tracingFactory.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.UnsubscribeToStateUpdates);
			tracingFactory.injectMsg(msg);
			serverSide.tell(msg, self);
		}
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				publishNewState(ClientSideStates.STOPPED);
			}
		}, context().system().dispatcher());
	}
}
