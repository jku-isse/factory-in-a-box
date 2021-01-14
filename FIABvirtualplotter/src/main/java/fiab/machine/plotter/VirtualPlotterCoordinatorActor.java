package fiab.machine.plotter;

import java.time.Duration;

import actuators.Motor;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import config.HardwareInfo;
import main.java.fiab.core.capabilities.BasicMachineStates;
import main.java.fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import main.java.fiab.core.capabilities.basicmachine.events.MachineInWrongStateResponse;
import main.java.fiab.core.capabilities.basicmachine.events.MachineStatusUpdateEvent;
import main.java.fiab.core.capabilities.handshake.IOStationCapability;
import main.java.fiab.core.capabilities.plotting.PlotterMessageTypes;
import main.java.fiab.core.capabilities.handshake.HandshakeCapability;
import main.java.fiab.core.capabilities.handshake.HandshakeCapability.ServerSideStates;
import fiab.handshake.actor.LocalEndpointStatus;
import fiab.handshake.actor.ServerSideHandshakeActor;
import hardware.ConveyorHardware;
import hardware.PlotterHardware;
import sensors.Sensor;

public class VirtualPlotterCoordinatorActor extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	protected IntraMachineEventBus intraEventBus;
	protected BasicMachineStates currentState = BasicMachineStates.STOPPING;
	protected boolean doPublishState = false;
	protected ServerSideStates handshakeStatus;
	protected ActorRef serverSide;
	protected ActorRef self;
	protected Motor conveyorMotor;
	protected Motor plotXMotor;
	protected Motor plotYMotor;
	protected Motor penMotor;
	protected Sensor conveyorSensorLoading;
	protected Sensor conveyorSensorUnloading;
	protected Sensor plotXSensor;
	protected Sensor plotYSensor;

	static public Props propsForLateHandshakeBinding(IntraMachineEventBus internalMachineEventBus,
			HardwareInfo hardwareInfo) {
		return Props.create(VirtualPlotterCoordinatorActor.class,
				() -> new VirtualPlotterCoordinatorActor(internalMachineEventBus, hardwareInfo, true));
	}

	static public Props props(IntraMachineEventBus internalMachineEventBus, HardwareInfo hardwareInfo) {
		return Props.create(VirtualPlotterCoordinatorActor.class,
				() -> new VirtualPlotterCoordinatorActor(internalMachineEventBus, hardwareInfo, false));
	}

	public VirtualPlotterCoordinatorActor(IntraMachineEventBus machineEventBus, HardwareInfo hardwareInfo,
			boolean doLateBinding) {
		this.intraEventBus = machineEventBus;
		// setup serverhandshake actor with autocomplete
		self = getSelf();
		// serverSide = getContext().actorOf(MockServerHandshakeActor.props(getSelf(),
		// doAutoComplete).withDispatcher(CallingThreadDispatcher.Id()),
		// "ServerSideHandshakeMock");
		if (!doLateBinding) {
			boolean doAutoComplete = true;
			serverSide = getContext().actorOf(ServerSideHandshakeActor.props(getSelf(), doAutoComplete),
					"ServerSideHandshakeMock");
			this.setAndPublishState(BasicMachineStates.STOPPED);
		}
		ConveyorHardware conveyorHardware = hardwareInfo.getConveyorHardware().get(); // Assume hardware is available.
																						// Better to throw exception
																						// here than later
		this.conveyorMotor = conveyorHardware.getConveyorMotor();
		this.conveyorSensorLoading = conveyorHardware.getLoadingSensor();
		this.conveyorSensorUnloading = conveyorHardware.getUnloadingSensor();
		PlotterHardware plottingHardware = hardwareInfo.getPlotterHardware().get();
		this.plotXMotor = plottingHardware.getMotorX();
		this.plotYMotor = plottingHardware.getMotorY();
		this.penMotor = plottingHardware.getPenMotor();
		this.plotXSensor = plottingHardware.getSensorX();
		this.plotYSensor = plottingHardware.getSensorY();
	}

	public Receive createReceive() {
		return receiveBuilder().match(PlotterMessageTypes.class, msg -> {
			switch (msg) {
			case SubscribeState:
				doPublishState = true;
				setAndPublishState(currentState); // we publish the current state
				break;
			case Plot:
				if (currentState.equals(BasicMachineStates.IDLE))
					plot();
				else
					log.warning("VirtualPlotterCoordinatorActor told to plot in wrong state " + currentState);
				sender().tell(new MachineInWrongStateResponse("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME,
						"Machine not in state to plot", currentState, PlotterMessageTypes.Plot,
						BasicMachineStates.IDLE), self);
				break;
			case Reset:
				if (currentState.equals(BasicMachineStates.STOPPED))
					reset();
				else
					log.warning("Wrapper told to reset in wrong state " + currentState);
				break;
			case Stop:
				stop();
				break;
			case SetCapability:
				updateCapability("Updated");
				break;
			default:
				break;
			}
		}).match(ServerSideStates.class, msg -> { // state event updates
			log.info(String.format("Received %s from %s", msg, getSender()));
			// if (getSender().equals(serverSide)) {
			handshakeStatus = msg;
			switch (msg) {
			case COMPLETE: // handshake complete, thus un/loading done
				if (currentState.equals(BasicMachineStates.STARTING)) { // pallet is now loaded
					// transitionStartingToExecute();
					load();
				} else if (currentState.equals(BasicMachineStates.COMPLETING)) { // pallet is now unloaded
					// transitionCompletingToComplete();
					unload();
				}
				break;
			case STOPPED:
				if (currentState.equals(BasicMachineStates.STOPPING)) { // only if we wait for FU to stop, alternative
																		// way to learn about serverside
					if (serverSide == null) {
						setServerHandshakeActor(getSender()); // will also result in transition to stop
					} else
						transitionToStop();
				}
				break;
			default: // irrelevant states
				break;
			}
			// } else {
			// log.warning(String.format("Received %s from unexpected sender %s", msg,
			// getSender()));
			// }
		}).match(ActorRef.class, lateBoundHandshake -> {
			setServerHandshakeActor(lateBoundHandshake); // wont be called when serverhandshake announces itself to its
															// parentActor, and parentActor is set to this actor
		}).match(LocalEndpointStatus.LocalServerEndpointStatus.class, les -> {
			setServerHandshakeActor(les.getActor());
		}).matchAny(msg -> {
			log.warning("Unexpected Message received: " + msg.toString());
		}).build();
	}

	private void setServerHandshakeActor(ActorRef serverSide) {
		if (this.currentState.equals(BasicMachineStates.STOPPING)) {
			this.serverSide = serverSide;
			setAndPublishState(BasicMachineStates.STOPPED);
		}
	}

	private void updateCapability(String newCapability) {
		intraEventBus.publish(new MachineCapabilityUpdateEvent("", "Plot_Capability", newCapability));
	}

	private void setAndPublishState(BasicMachineStates newState) {
		// log.debug(String.format("%s sets state from %s to %s",
		// this.machineId.getId(), this.currentState, newState));
		this.currentState = newState;
		if (doPublishState) {
			intraEventBus.publish(
					new MachineStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", newState));
		}
	}

	private void reset() {
		log.info("Resetting hardware...");
		setAndPublishState(BasicMachineStates.RESETTING);
		plotXMotor.backward();
		plotYMotor.backward();
		conveyorMotor.stop();
		waitForHardwareToReset();
	}

	private void waitForHardwareToReset() {
		if (plotXSensor.hasDetectedInput() && plotYSensor.hasDetectedInput()) {
			context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), () -> {
				log.info("Homing position reached");
				plotXMotor.stop();
				plotYMotor.stop();
				setAndPublishState(BasicMachineStates.IDLE);
			}, context().system().dispatcher());
		} else {
			context().system().scheduler().scheduleOnce(Duration.ofMillis(100), () -> {
				waitForHardwareToReset();
			}, context().dispatcher());
		}
	}

	private void stop() {
		setAndPublishState(BasicMachineStates.STOPPING);
		log.info("Stopping motors...");
		conveyorMotor.stop();
		plotXMotor.stop();
		plotYMotor.stop();
		serverSide.tell(IOStationCapability.ServerMessageTypes.Stop, getSelf());
		context().system().scheduler().scheduleOnce(Duration.ofMillis(1000), new Runnable() {
			@Override
			public void run() {
				if (handshakeStatus.equals(ServerSideStates.STOPPED)) {
					log.info("Plotter motors stopped");
					transitionToStop();
				}
			}
		}, context().system().dispatcher());
	}

	private void transitionToStop() {
		setAndPublishState(BasicMachineStates.STOPPED);
	}

	private void plot() {
		setAndPublishState(BasicMachineStates.STARTING);
		sender().tell(new MachineStatusUpdateEvent("", OPCUABasicMachineBrowsenames.STATE_VAR_NAME, "", currentState),
				self);
		// now here we also enable pallet to be loaded onto machine
		serverSide.tell(HandshakeCapability.ServerMessageTypes.Reset, self);
	}

	private void load() {
		setAndPublishState(BasicMachineStates.EXECUTE);
		log.info("Loading...");
		// context().system().scheduler().scheduleOnce(Duration.ofSeconds(5), new
		// Runnable() {
		// @Override
		// public void run() {
		conveyorMotor.backward(); // backwards loads the pattet onto the conveyor
		waitForLoadingToFinish();
		// finishProduction();
		// }
		// }, context().system().dispatcher());
	}

	private void waitForLoadingToFinish() {
		if (conveyorSensorLoading.hasDetectedInput()) {
			log.debug("ConveyorLoading: {}, ConveyorUnloading: {}, transitioning to Execute", conveyorSensorLoading,
					conveyorSensorUnloading);
			log.info("Loading finshed");
			conveyorMotor.stop();
			// finishProduction();
			doActualPlotting();
		} else {
			context().system().scheduler().scheduleOnce(Duration.ofMillis(100), () -> {
				waitForLoadingToFinish();
			}, context().dispatcher());
		}
	}

	private void doActualPlotting() { // here we move the motors into starting position and put the pen onto the paper
		log.info("Bringing Motors in position...");
		plotXMotor.forward();
		plotYMotor.forward();
		context().system().scheduler().scheduleOnce(Duration.ofSeconds(3), () -> {
			log.info("Moving Pen Motor down...");
			plotXMotor.stop();
			plotYMotor.stop();
			penMotor.forward();
			drawImage();
		}, context().dispatcher());

	}

	private void drawImage() { // here we start plotting a diagonal, then stop the motors and move pen up again
		log.info("Drawing Image...");
		penMotor.stop();
		plotXMotor.forward();
		plotYMotor.forward();
		context().system().scheduler().scheduleOnce(Duration.ofSeconds(3), () -> {
			log.info("Drawing finshed, lifting Pen...");
			plotXMotor.stop();
			plotYMotor.stop();
			penMotor.backward();
			finishPlotting();
		}, context().dispatcher());
	}

	private void finishPlotting() { // when the pen motor is up, start homing plot motors and wait for homing pos
									// reached
		log.info("Plotting process done, homing plot motors...");
		context().system().scheduler().scheduleOnce(Duration.ofSeconds(3), () -> {
			penMotor.stop();
			plotXMotor.backward();
			plotYMotor.backward();
			waitForPlotterHWToReset();
		}, context().dispatcher());
	}

	private void waitForPlotterHWToReset() { // when homing position reached, finish production
		if (plotXSensor.hasDetectedInput() && plotYSensor.hasDetectedInput()) {
			log.info("Homing position reached");
			plotXMotor.stop();
			plotYMotor.stop();
			finishProduction();
		} else {
			context().system().scheduler().scheduleOnce(Duration.ofMillis(100), () -> {
				waitForPlotterHWToReset();
			}, context().dispatcher());
		}
	}

	private void finishProduction() {
		setAndPublishState(BasicMachineStates.COMPLETING);
		serverSide.tell(IOStationCapability.ServerMessageTypes.Reset, self); // now again do a handshake and unload,
		/*
		 * context().system().scheduler().scheduleOnce(Duration.ofMillis(3000), new
		 * Runnable() {
		 * 
		 * @Override public void run() { if
		 * (handshakeStatus.equals(ServerSideStates.COMPLETE)) {
		 * conveyorMotor.forward(); // forward moves pallet out of conveyor
		 * waitForUnloadingToFinish(); } } }, context().system().dispatcher());
		 */
	}

	private void unload() {
		log.info("Unloading...");
		conveyorMotor.forward(); // forward moves pallet out of conveyor
		waitForUnloadingToFinish();
	}

	private void waitForUnloadingToFinish() {
		if (!conveyorSensorLoading.hasDetectedInput() && !conveyorSensorUnloading.hasDetectedInput()) {
			log.debug("ConveyorLoading: {}, ConveyorUnloading: {}, transitioning to Complete", conveyorSensorLoading,
					conveyorSensorUnloading);
			log.info("Unloading finished");
			conveyorMotor.stop();
			transitionCompletingToComplete();
		} else {
			context().system().scheduler().scheduleOnce(Duration.ofMillis(100), () -> {
				waitForUnloadingToFinish();
			}, context().dispatcher());
		}
	}

	private void transitionCompletingToComplete() {
		setAndPublishState(BasicMachineStates.COMPLETE);
		context().system().scheduler().scheduleOnce(Duration.ofSeconds(1), () -> {
			reset(); // we automatically reset
		}, context().dispatcher());
		
	}

}
