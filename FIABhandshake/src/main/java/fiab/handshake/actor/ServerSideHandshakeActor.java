package fiab.handshake.actor;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.core.capabilities.handshake.HandshakeCapability.StateOverrideRequests;
import fiab.core.capabilities.handshake.IOStationCapability;
import fiab.handshake.actor.messages.HSServerMessage;
import fiab.handshake.actor.messages.HSServerSideStateMessage;
import fiab.handshake.actor.messages.HSStateOverrideRequestMessage;
import fiab.tracing.actor.AbstractTracingActor;

public class ServerSideHandshakeActor extends AbstractTracingActor {
	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected boolean isLoaded = false; // assume at bootup that no pallet is loaded
	private ActorRef parentActor;
	protected ServerSideStates currentState = ServerSideStates.STOPPED;
	// protected ActorRef clientSide;
	protected ActorRef self;
	protected Set<ActorRef> subscribers = new HashSet<ActorRef>();
	protected boolean doAutoComplete = false;
	protected StatePublisher publishEP;

	static public Props props(ActorRef parent, boolean doAutoComplete) {
		return Props.create(ServerSideHandshakeActor.class,
				() -> new ServerSideHandshakeActor(parent, doAutoComplete, null));
	}

	static public Props props(ActorRef parent, boolean doAutoComplete, StatePublisher publishEP) {
		return Props.create(ServerSideHandshakeActor.class,
				() -> new ServerSideHandshakeActor(parent, doAutoComplete, publishEP));
	}

	public ServerSideHandshakeActor(ActorRef machineWrapper, boolean doAutoComplete, StatePublisher publishEP) {
		this.parentActor = machineWrapper;
		this.doAutoComplete = doAutoComplete;
		this.publishEP = publishEP;
		self = getSelf();
		publishNewState(currentState);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(IOStationCapability.ServerMessageTypes.class, body -> {
			receiveServerMessage(new HSServerMessage("", body));

		}).match(HSServerMessage.class, msg -> {
			receiveServerMessage(msg);

		}).match(StateOverrideRequests.class, req -> {
			receiveStateOverrideRequest(new HSStateOverrideRequestMessage("", req));

		}).match(HSStateOverrideRequestMessage.class, msg -> {
			receiveStateOverrideRequest(msg);

		}).matchAny(msg -> {
			log.warning(String.format("Unexpected Message received <%s> from %s", msg.toString(), getSender()));
		}).build();
	}

	private void receiveStateOverrideRequest(HSStateOverrideRequestMessage msg) {
		StateOverrideRequests body = msg.getBody();

		log.info(String.format("Received %s from %s", body, getSender()));
		try {
			tracer.startConsumerSpan(msg,
					"Server-Handshake: StateOverriderequest: " + body.toString() + " received");
			switch (body) {
			case SetLoaded:
				updateLoadState(true);
				break;
			case SetEmpty:
				updateLoadState(false);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			tracer.finishCurrentSpan();
		}

	}

	private void receiveServerMessage(HSServerMessage msg) {
		IOStationCapability.ServerMessageTypes body = msg.getBody();

		log.info(String.format("Received %s from %s", body, getSender()));

		try {
			tracer.startConsumerSpan(msg,
					"Server-Handshake: " + body.toString() + " received");
			switch (body) {
			case Complete:
				if (currentState.equals(ServerSideStates.EXECUTE))
					complete();
				break;
			case RequestInitiateHandover:
				initHandover();
				break;
			case RequestStartHandover:
				startHandover();
				break;
			case Reset:
				if (currentState.equals(ServerSideStates.STOPPED) || currentState.equals(ServerSideStates.COMPLETE))
					reset();
				break;
			case Stop:
				if (currentState.equals(ServerSideStates.STOPPED) || currentState.equals(ServerSideStates.COMPLETE))
					stop();
				break;
			case SubscribeToStateUpdates:
				if (getSender() != context().system().deadLetters()) {
					subscribers.add(getSender());
					getSender().tell(currentState, getSelf()); // update subscriber with current state
				} else {
					publishNewState(currentState);
				}
				break;
			case UnsubscribeToStateUpdates:
				subscribers.remove(getSender());
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			tracer.finishCurrentSpan();
		}
	}

	protected void publishNewState(ServerSideStates newState) {
		currentState = newState;
		HSServerSideStateMessage msg = new HSServerSideStateMessage(tracer.getCurrentHeader(), newState);
		tracer.injectMsg(msg);

		if (parentActor != null) {
			parentActor.tell(msg, self);
		}
		if (publishEP != null) {
			publishEP.setStatusValue(newState.toString());
		}
		// sending extensible message to all subscribers
		subscribers.stream().forEach(sub -> sub.tell(msg, self));
	}

	private Set<ServerSideStates> loadChangeableStates = Sets.newHashSet(ServerSideStates.COMPLETE,
			ServerSideStates.COMPLETING, ServerSideStates.STOPPED, ServerSideStates.STOPPING);

	protected boolean updateLoadState(boolean isLoaded) {
//		if (this.isLoaded != isLoaded) {
//			this.isLoaded = isLoaded;
//			if (currentState != ServerSideStates.STOPPED)
//				stop();
//		}
		// allow only when in stopped:
		if (loadChangeableStates.contains(currentState)) {
			this.isLoaded = isLoaded;
			return true;
		}
		return false;
	}

	protected void reset() {
		publishNewState(ServerSideStates.RESETTING);
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				if (isLoaded) {
					publishNewState(ServerSideStates.IDLE_LOADED);
				} else {
					publishNewState(ServerSideStates.IDLE_EMPTY);
				}
			}
		}, context().system().dispatcher());
	}

	private void initHandover() {
		if (currentState.equals(ServerSideStates.IDLE_EMPTY) || currentState.equals(ServerSideStates.IDLE_LOADED)) {
			publishNewState(ServerSideStates.STARTING);
			log.info(String.format("Responding with OkResponseInitHandover to %s", getSender()));

			HSServerMessage msg = new HSServerMessage(tracer.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.OkResponseInitHandover);
			tracer.injectMsg(msg);
			getSender().tell(msg, self);
			context().system().scheduler().scheduleOnce(Duration.ofMillis(200), new Runnable() {
				@Override
				public void run() {
					publishNewState(ServerSideStates.PREPARING);
					if (isLoaded) {
						publishNewState(ServerSideStates.READY_LOADED);
					} else {
						publishNewState(ServerSideStates.READY_EMPTY);
					}
				}
			}, context().system().dispatcher());
		} else if (currentState.equals(ServerSideStates.READY_EMPTY)
				|| currentState.equals(ServerSideStates.READY_LOADED)) {
			HSServerMessage msg = new HSServerMessage(tracer.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.OkResponseInitHandover);
			tracer.injectMsg(msg);
			getSender().tell(msg, self); // resending
		} else {
			log.warning(String.format("Responding with NotOkResponseInitHandover to %s in state %s", getSender(),
					currentState));
			HSServerMessage msg = new HSServerMessage(tracer.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.NotOkResponseInitHandover);
			tracer.injectMsg(msg);
			getSender().tell(msg, self);
		}

	}

	private void startHandover() {
		if ((currentState.equals(ServerSideStates.READY_EMPTY) || currentState.equals(ServerSideStates.READY_LOADED))) {
			publishNewState(ServerSideStates.EXECUTE);
			log.info(String.format("Responding with OkResponseStartHandover to %s", getSender()));
			HSServerMessage msg = new HSServerMessage(tracer.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.OkResponseStartHandover);
			tracer.injectMsg(msg);
			getSender().tell(msg, self);
		} else if (currentState.equals(ServerSideStates.EXECUTE)) { // resending
			HSServerMessage msg = new HSServerMessage(tracer.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.OkResponseStartHandover);
			tracer.injectMsg(msg);
			getSender().tell(msg, self);
		} else {
			log.warning(String.format("Responding with NotOkResponseStartHandover to %s in state %s", getSender(),
					currentState));
			HSServerMessage msg = new HSServerMessage(tracer.getCurrentHeader(),
					IOStationCapability.ServerMessageTypes.NotOkResponseStartHandover);
			tracer.injectMsg(msg);
			getSender().tell(msg, self);
		}
		if (doAutoComplete) {
			context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
				@Override
				public void run() {
					complete();
				}
			}, context().system().dispatcher());
		}
	}

	private void complete() {
		publishNewState(ServerSideStates.COMPLETING);
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				// we toggle load flag as when we were empty now we are loaded and vice versa
				isLoaded = !isLoaded;
				publishNewState(ServerSideStates.COMPLETE);
//            	stop(); // we automatically stop --> no longer stop, just remain in complete
			}
		}, context().system().dispatcher());
	}

	private void stop() {
		publishNewState(ServerSideStates.STOPPING);
		// clientSide = null;
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				publishNewState(ServerSideStates.STOPPED);
			}
		}, context().system().dispatcher());
	}

}