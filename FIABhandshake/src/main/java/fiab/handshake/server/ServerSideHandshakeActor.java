package fiab.handshake.server;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.functionalunit.BasicFUBehaviour;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.handshake.server.*;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.handshake.server.statemachine.ServerSideHandshakeTriggers;
import fiab.handshake.server.statemachine.ServerSideStateMachine;

public class ServerSideHandshakeActor extends AbstractActor implements BasicFUBehaviour, StatePublisher {

    public static Props props(FUConnector requestBus, IntraMachineEventBus intraMachineEventBus,
                              ServerResponseConnector responseConnector, ServerNotificationConnector notificationConnector) {
        return Props.create(ServerSideHandshakeActor.class,
                () -> new ServerSideHandshakeActor(requestBus, intraMachineEventBus, responseConnector, notificationConnector));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final String componentId;
    private final TransportAreaStatusInfo info;

    protected final ServerSideStateMachine stateMachine;
    protected final IntraMachineEventBus intraMachineEventBus;
    protected final FUConnector requestBus;
    protected final ServerResponseConnector responseConnector;
    protected final ServerNotificationConnector notificationConnector;

    public ServerSideHandshakeActor(FUConnector requestBus, IntraMachineEventBus intraMachineEventBus,
                                    ServerResponseConnector responseConnector, ServerNotificationConnector notificationConnector) {
        this.componentId = self().path().name();
        this.info = new TransportAreaStatusInfo();
        this.stateMachine = new ServerSideStateMachine(this.info);
        this.intraMachineEventBus = intraMachineEventBus;
        this.requestBus = requestBus;
        this.responseConnector = responseConnector;
        this.notificationConnector = notificationConnector;
        addActionsToStates();
        subscribeToConnectors();
        publishCurrentState(stateMachine.getState());   //First state does not trigger onEntry
    }

    protected void addActionsToStates() {
        for (ServerSideStates state : ServerSideStates.values()) {
            stateMachine.configure(state).onEntry(() -> publishCurrentState(state));
        }
        stateMachine.configure(ServerSideStates.RESETTING).onEntry(() -> doResetting());
        stateMachine.configure(ServerSideStates.STOPPING).onEntry(() -> doStopping());
        stateMachine.configure(ServerSideStates.PREPARING).onEntry(() -> finishPreparations());
        stateMachine.configure(ServerSideStates.EXECUTE).onEntry(() -> doExecute());
        stateMachine.configure(ServerSideStates.COMPLETING).onEntry(() -> completeHandshake());
    }

    private void subscribeToConnectors() {
        this.requestBus.subscribe(self(), new FUSubscriptionClassifier(this.componentId, "*"));
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(StopRequest.class, req -> stateMachine.fireIfPossible(ServerSideHandshakeTriggers.STOP))
                .match(ResetRequest.class, req -> stateMachine.fireIfPossible(ServerSideHandshakeTriggers.RESET))
                .match(InitiateHandoverRequest.class, req -> initHandshake())
                .match(StartHandoverRequest.class, req -> startHandshake())
                .match(CompleteHandshake.class, req -> stateMachine.fireIfPossible(ServerSideHandshakeTriggers.COMPLETE))
                .match(TransportAreaStatusOverrideRequest.class, req -> info.updateTransportAreaStatus(req.getOverrideRequest()))
                .match(SubscribeToUpdatesRequest.class, req -> {
                            log.info("Received remote subscription from " + req.getSenderId());
                            publishCurrentState(stateMachine.getState());
                        }
                )
                .match(UnsubscribeToUpdatesRequest.class, req -> {
                    log.info("Remote " + req.getSenderId() + " unsubscribed");
                })
                .build();
    }

    public void proceedToIdle() {
        stateMachine.fireIfPossible(ServerSideHandshakeTriggers.RESETTING_DONE);
    }

    public void initHandshake() {
        try {
            stateMachine.fire(ServerSideHandshakeTriggers.START);
            publishServerResponseEvent(HandshakeCapability.ServerMessageTypes.OkResponseInitHandover);
        } catch (IllegalStateException e) {
            log.warning("Could not initialize handshake due to " + e.getMessage() + ". Stopping handshake component.");
            publishServerResponseEvent(HandshakeCapability.ServerMessageTypes.NotOkResponseInitHandover);
            stateMachine.fireIfPossible(ServerSideHandshakeTriggers.STOP);
        }
        stateMachine.fireIfPossible(ServerSideHandshakeTriggers.PREPARE);
    }

    public void startHandshake() {
        try {
            stateMachine.fire(ServerSideHandshakeTriggers.EXECUTE);
            if(stateMachine.isInState(ServerSideStates.STOPPING) || stateMachine.isInState(ServerSideStates.STOPPED)){
                log.warning("Could not start handshake, check whether the loading status during init/ready match");
                publishServerResponseEvent(HandshakeCapability.ServerMessageTypes.NotOkResponseStartHandover);
            }else {
                publishServerResponseEvent(HandshakeCapability.ServerMessageTypes.OkResponseStartHandover);
            }
        } catch (IllegalStateException e) {
            log.warning("Could not start handshake due to " + e.getMessage() + ". Stopping handshake component.");
            publishServerResponseEvent(HandshakeCapability.ServerMessageTypes.NotOkResponseStartHandover);
            stateMachine.fireIfPossible(ServerSideHandshakeTriggers.STOP);
        }
    }

    public void finishPreparations() {
        stateMachine.fireIfPossible(ServerSideHandshakeTriggers.READY);
    }

    public void doExecute(){
        //Do nothing in here, since we just wait for completing signal
    }

    public void completeHandshake() {
        stateMachine.fireIfPossible(ServerSideHandshakeTriggers.COMPLETING_DONE);
    }

    @Override
    public void doResetting() {
        proceedToIdle();
    }

    @Override
    public void doStopping() {
        proceedToStopped();
    }

    public void proceedToStopped() {
        stateMachine.fireIfPossible(ServerSideHandshakeTriggers.STOPPING_DONE);
    }

    public void publishServerResponseEvent(HandshakeCapability.ServerMessageTypes response) {
        if (responseConnector != null) {
            responseConnector.publish(new ServerHandshakeResponseEvent(this.componentId, response));
        }
    }

    public void publishCurrentState(ServerSideStates state) {
        if (intraMachineEventBus != null) {
            intraMachineEventBus.publish(new ServerHandshakeStatusUpdateEvent(this.componentId, state));
        }
        if (notificationConnector != null) {
            notificationConnector.publish(new ServerHandshakeStatusUpdateEvent(this.componentId, state));
        }
        setStatusValue(state.toString());
    }

    @Override
    public void setStatusValue(String newStatus) {
        log.info("Publishing new State: " + newStatus);
    }
}
