package fiab.turntable.turning;

import static fiab.turntable.turning.statemachine.TurningStates.STOPPED;
import static fiab.turntable.turning.statemachine.TurningTriggers.RESET;
import static fiab.turntable.turning.statemachine.TurningTriggers.STOP;
import static fiab.turntable.turning.statemachine.TurningTriggers.TURN_TO;

import java.time.Duration;

import com.github.oxo42.stateless4j.StateMachine;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.StatePublisher;
import fiab.tracing.actor.AbstractTracingActor;
import fiab.turntable.actor.IntraMachineEventBus;
import fiab.turntable.actor.messages.TurningTriggerMessage;
import fiab.turntable.turning.statemachine.TurningStateMachineConfig;
import fiab.turntable.turning.statemachine.TurningStates;
import fiab.turntable.turning.statemachine.TurningTriggers;

public class BaseBehaviorTurntableActor extends AbstractTracingActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	protected IntraMachineEventBus intraEventBus;
	protected StatePublisher publishEP;

	protected StateMachine<TurningStates, TurningTriggers> tsm;

	public static Props props(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
		return Props.create(BaseBehaviorTurntableActor.class,
				() -> new BaseBehaviorTurntableActor(intraEventBus, publishEP));
	}

	public BaseBehaviorTurntableActor(IntraMachineEventBus intraEventBus, StatePublisher publishEP) {
		this.intraEventBus = intraEventBus;
		this.publishEP = publishEP;
		this.tsm = new StateMachine<>(STOPPED, new TurningStateMachineConfig());
		publishNewState();
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(TurningTriggerMessage.class, msg -> {
			receiveTurningTrigger(msg);

		}).match(TurningTriggers.class, req -> {
			receiveTurningTrigger(new TurningTriggerMessage("", req));

		}).match(TurnRequest.class, req -> {
			try {
				tracer.startConsumerSpan(req, "Turn Request received");
				if (tsm.canFire(TURN_TO)) {
					tsm.fire(TURN_TO); // in STARTING
					publishNewState();
					turn(req);
				} else {
					log.warning("Turntable not ready for TurningRequest to: " + req.getTto());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracer.finishCurrentSpan();
			}

		}).matchAny(msg -> {
			log.warning("Unexpected Message received: " + msg.toString());
		}).build();
	}

	private void receiveTurningTrigger(TurningTriggerMessage msg) {
		TurningTriggers trigger = msg.getBody();
		try {
			tracer.startConsumerSpan(msg, "Turning Actor: Trigger " + trigger.toString() + " received");
			switch (trigger) {
			case STOP:
				if (tsm.canFire(STOP)) {
					tsm.fire(STOP);
					publishNewState(); // in STOPPING
					stop();
				}
				break;
			case RESET:
				if (tsm.canFire(RESET)) {
					tsm.fire(RESET); // in RESETTING
					publishNewState();
					reset();
				}
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

	protected void publishNewState() {
		if (publishEP != null)
			publishEP.setStatusValue(tsm.getState().toString());
		if (intraEventBus != null) {
			TurntableStatusUpdateEvent event = new TurntableStatusUpdateEvent("",
					OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", tsm.getState());
			event.setTracingHeader(tracer.getCurrentHeader());
			tracer.injectMsg(event);

			intraEventBus.publish(event);
		}
	}

	protected void stop() {
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				tsm.fire(TurningTriggers.NEXT);
				publishNewState();
			}
		}, context().system().dispatcher());
	}

	protected void reset() {
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				tsm.fire(TurningTriggers.NEXT);
				publishNewState();
			}
		}, context().system().dispatcher());
	}

	protected void turn(TurnRequest treq) {
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				tsm.fire(TurningTriggers.EXECUTE);
				// Do actual turning here
				publishNewState();
				completing();
			}
		}, context().system().dispatcher());
	}

	protected void completing() {
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				tsm.fire(TurningTriggers.NEXT);
				publishNewState(); // we are now in COMPETING
				complete();
			}
		}, context().system().dispatcher());
	}

	protected void complete() {
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				tsm.fire(TurningTriggers.NEXT);
				publishNewState(); // we are now in COMPETE
				autoResetToIdle();
			}
		}, context().system().dispatcher());
	}

	protected void autoResetToIdle() {
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				tsm.fire(TurningTriggers.NEXT);
				publishNewState(); // we are now in IDLE
			}
		}, context().system().dispatcher());
	}

}
