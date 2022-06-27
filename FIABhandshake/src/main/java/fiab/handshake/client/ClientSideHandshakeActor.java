package fiab.handshake.client;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.core.capabilities.StatePublisher;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.client.CompleteHandshake;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.functionalunit.connector.IntraMachineEventBus;
import fiab.core.capabilities.functionalunit.BasicFUBehaviour;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.functionalunit.StopRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.core.capabilities.handshake.ClientSideStates;
import fiab.core.capabilities.handshake.client.PerformHandshake;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.core.capabilities.handshake.server.SubscribeToUpdatesRequest;
import fiab.core.capabilities.handshake.server.UnsubscribeToUpdatesRequest;
import fiab.handshake.client.messages.ClientHandshakeStatusUpdateEvent;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.opcua.RemoteServerHandshakeNodeIds;
import fiab.handshake.client.opcua.client.ClientSpawnerMessages;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;
import fiab.handshake.client.statemachine.ClientSideHandshakeTriggers;
import fiab.handshake.client.statemachine.ClientSideStateMachine;

import java.time.Duration;

public class ClientSideHandshakeActor extends AbstractActor implements BasicFUBehaviour, StatePublisher {

    public static Props props(FUConnector requestBus, IntraMachineEventBus intraMachineEventBus, FUConnector serverRequestBus,
                              ServerResponseConnector serverResponseBus,
                              ServerNotificationConnector serverNotificationBus) {

        return Props.create(ClientSideHandshakeActor.class, () -> new ClientSideHandshakeActor(requestBus, intraMachineEventBus,
                serverRequestBus, serverResponseBus, serverNotificationBus, false));
    }

    /**
     * This factory will create a ClientSideHandshakeActor which skips the Client initialization during reset and
     * immediately proceeds to the idle state
     * @return Local ClientSide HandshakeActor
     */
    public static Props propsLocalHandshake(FUConnector requestBus, IntraMachineEventBus intraMachineEventBus, FUConnector serverRequestBus,
                                            ServerResponseConnector serverResponseBus,
                                            ServerNotificationConnector serverNotificationBus) {

        return Props.create(ClientSideHandshakeActor.class, () -> new ClientSideHandshakeActor(requestBus, intraMachineEventBus,
                serverRequestBus, serverResponseBus, serverNotificationBus, true));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    protected final String componentId;
    protected final IntraMachineEventBus intraMachineEventBus;
    private final FUConnector requestBus;
    private final FUConnector serverRequestBus;
    private final ServerNotificationConnector serverNotificationBus;
    private final ServerResponseConnector serverResponseBus;

    private final RemoteServerHandshakeInfo remoteHsInfo;
    protected final ClientSideStateMachine stateMachine;

    private Cancellable initRetryTask;
    private Cancellable startRetryTask;

    protected WiringInfo wiringInfo;
    protected RemoteServerHandshakeNodeIds nodeIds;
    private final boolean isLocalHandshake;

    public ClientSideHandshakeActor(FUConnector requestBus, IntraMachineEventBus intraMachineEventBus, FUConnector serverRequestBus,
                                    ServerResponseConnector serverResponseBus,
                                    ServerNotificationConnector serverNotificationBus, boolean isLocalHandshake) {
        this.componentId = self().path().name();
        remoteHsInfo = new RemoteServerHandshakeInfo();
        stateMachine = new ClientSideStateMachine(remoteHsInfo);
        addActionsToState();
        this.intraMachineEventBus = intraMachineEventBus;   //State updates to machine
        this.requestBus = requestBus;                       //Requests coming from machine
        this.serverRequestBus = serverRequestBus;           //Requests to remote server hs
        this.serverResponseBus = serverResponseBus;         //Responses to remote server hs requests
        this.serverNotificationBus = serverNotificationBus; //State updates from server hs
        subscribeToEvents();
        this.isLocalHandshake = isLocalHandshake;
        publishCurrentState();
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                //compatibility with previous version
                .match(HandshakeCapability.ClientMessageTypes.class, req -> {
                    if (req == HandshakeCapability.ClientMessageTypes.Reset) {
                        stateMachine.fireIfPossible(ClientSideHandshakeTriggers.RESET);
                    } else if (req == HandshakeCapability.ClientMessageTypes.Stop) {
                        stateMachine.fireIfPossible(ClientSideHandshakeTriggers.STOP);
                    }
                })
                //FU Requests
                .match(ResetRequest.class, msg -> stateMachine.fireIfPossible(ClientSideHandshakeTriggers.RESET))
                .match(StopRequest.class, msg -> stateMachine.fireIfPossible(ClientSideHandshakeTriggers.STOP))
                .match(PerformHandshake.class, req -> stateMachine.fireIfPossible(ClientSideHandshakeTriggers.START))
                .match(CompleteHandshake.class, req -> stateMachine.fireIfPossible(ClientSideHandshakeTriggers.COMPLETE))
                //Remote notifications
                .match(ServerHandshakeResponseEvent.class, resp -> {
                    log.debug("Server Response:" + resp);
                    remoteHsInfo.setServerResponse(resp.getResponse());
                })
                .match(ServerHandshakeStatusUpdateEvent.class, msg -> {
                    log.debug("Remote Server Status update: " + msg);
                    remoteHsInfo.setRemoteState(msg.getStatus());
                })
                .match(WiringRequest.class, msg -> setWiringInfo(msg.getInfo()))
                .match(RemoteServerHandshakeNodeIds.class, msg -> registerServerHandshakeNodeIds(msg))
                .match(ClientSpawnerMessages.ClientCreationFailed.class, msg -> stateMachine.fireIfPossible(ClientSideHandshakeTriggers.STOP))
                .matchAny(msg -> log.debug("Could not process message: " + msg))
                .build();
    }

    protected void addActionsToState() {
        for (ClientSideStates state : ClientSideStates.values()) {
            stateMachine.configure(state).onEntry(this::publishCurrentState);
        }
        stateMachine.configure(ClientSideStates.RESETTING).onEntry(this::doResetting);
        stateMachine.configure(ClientSideStates.STOPPING).onEntry(this::doStopping);
        stateMachine.configure(ClientSideStates.STARTING).onEntry(this::startHandshake);
        stateMachine.configure(ClientSideStates.INITIATING)
                .onEntry(this::requestInitiateHandover)
                .onExit(() -> cancelInitRetry());
        stateMachine.configure(ClientSideStates.INITIATED);
        stateMachine.configure(ClientSideStates.READY)
                .onEntry(this::requestStartHandover)
                .onExit(() -> cancelStartRetry());
        stateMachine.configure(ClientSideStates.COMPLETING).onEntry(this::complete);
    }

    protected void subscribeToEvents() {
        this.requestBus.subscribe(self(), new FUSubscriptionClassifier(self().path().name(), "*"));
        this.serverResponseBus.subscribe(self(), new FUSubscriptionClassifier(self().path().name(), "*"));
        this.serverNotificationBus.subscribe(self(), new FUSubscriptionClassifier(self().path().name(), "*"));
    }

    /**
     * We store the wiringInfo in order to display it via OpcUa if necessary.
     * The wiringInfo can also be used to reconnect to the server after a crash
     *
     * @param info wiring info
     */
    protected void setWiringInfo(WiringInfo info) {
        this.wiringInfo = info;
    }

    /**
     * Here we register the server and proceed to initiating, since the proxy will start monitoring the variables
     *
     * @param nodeIds remote node ids
     */
    protected void registerServerHandshakeNodeIds(RemoteServerHandshakeNodeIds nodeIds) {
        this.nodeIds = nodeIds;
        stateMachine.fireIfPossible(ClientSideHandshakeTriggers.RESETTING_DONE);
    }

    /**
     * We start monitoring the remote state to check whether the remote handshake is in the correct state
     */
    protected void startHandshake() {
        serverRequestBus.publish(new SubscribeToUpdatesRequest(componentId));
        //stateMachine.fireIfPossible(ClientSideHandshakeTriggers.INITIATE);
    }

    /**
     * Here we send the first message to the server.
     * The state machine checks the remote state before sending the init request.
     * In case we don't receive a response, we resend the message until the server is stopped or init is successful
     */
    private void requestInitiateHandover() {
        initRetryTask = context().system().scheduler().scheduleWithFixedDelay(Duration.ofSeconds(0),
                Duration.ofSeconds(5),
                () -> serverRequestBus.publish(new InitiateHandoverRequest(componentId)),
                context().dispatcher());
    }

    /**
     * Upon successful init, we stop sending new init requests
     */
    private void cancelInitRetry(){
        if (initRetryTask != null) initRetryTask.cancel();
    }

    /**
     * Here we verify the server is ready.
     * The state machine checks the remote state before sending the request.
     * In case we don't receive a response, we resend the message until the server is stopped or start is successful
     */
    private void requestStartHandover() {
        this.stateMachine.fireIfPossible(ClientSideHandshakeTriggers.READY);
        //serverRequestBus.publish(new StartHandoverRequest(componentId));
        startRetryTask = context().system().scheduler().scheduleWithFixedDelay(Duration.ofSeconds(0),
                Duration.ofSeconds(5),
                () -> serverRequestBus.publish(new StartHandoverRequest(componentId)),
                context().dispatcher());
    }

    /**
     * Upon successful start request or leaving ready state, we can stop retry
     */
    private void cancelStartRetry(){
        if (startRetryTask != null) startRetryTask.cancel();
    }

    /**
     * We complete the handshake. Since we are done, we stop monitoring the remote state
     */
    private void complete() {
        serverRequestBus.publish(new UnsubscribeToUpdatesRequest(componentId));
        stateMachine.fireIfPossible(ClientSideHandshakeTriggers.COMPLETING_DONE);
    }

    /**
     * Override this method to create a client in the FU.
     * Once the client has been successfully created, reset_done will be called from registerServerHandshakeNodeIds.
     * This can be done manually or by sending a ServerHandshakeNodeIds message to this actor.
     */
    @Override
    public void doResetting() {
        if (isLocalHandshake) {
            stateMachine.fireIfPossible(ClientSideHandshakeTriggers.RESETTING_DONE);
        }
    }

    @Override
    public void doStopping() {
        serverRequestBus.publish(new UnsubscribeToUpdatesRequest(componentId));
        stateMachine.fireIfPossible(ClientSideHandshakeTriggers.STOPPING_DONE);
    }

    /**
     * Publishes the current state to the intraMachine eventBus. Otherwise, just the status value is set
     */
    public void publishCurrentState() {
        if (intraMachineEventBus != null) {
            intraMachineEventBus.publish(new ClientHandshakeStatusUpdateEvent(componentId, stateMachine.getState()));
        }
        setStatusValue(stateMachine.getState().toString());
    }

    @Override
    public void setStatusValue(String newStatus) {
        log.info("Publishing new state " + stateMachine.getState());
    }
}
