package fiab.handshake.client.opcua.clientproxy;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.core.capabilities.handshake.ServerSideStates;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.functionalunit.observer.FUStateObserver;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.core.capabilities.handshake.server.SubscribeToUpdatesRequest;
import fiab.core.capabilities.handshake.server.UnsubscribeToUpdatesRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.opcua.RemoteServerHandshakeNodeIds;
import fiab.handshake.connector.ServerNotificationConnector;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;
import fiab.handshake.server.messages.ServerHandshakeStatusUpdateEvent;


public class OpcUaServerHandshakeProxy extends AbstractActor implements FUStateObserver {

    public static Props props(ActorRef parent, FUConnector remoteRequestBus, ServerResponseConnector responseConnector,
                              ServerNotificationConnector notificationConnector) {
        return Props.create(OpcUaServerHandshakeProxy.class, () ->
                new OpcUaServerHandshakeProxy(parent, remoteRequestBus, responseConnector, notificationConnector));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final ActorRef parent;
    private final String componentId;
    private final FUConnector remoteRequestBus;
    private final ServerResponseConnector responseConnector;
    private final ServerNotificationConnector notificationConnector;

    private ActorRef spawnerActor;
    private WiringInfo wiringInfo;
    private RemoteServerHandshakeNodeIds remoteNodeIds;
    private boolean waitingForClientInstance;

    public OpcUaServerHandshakeProxy(ActorRef parent, FUConnector remoteRequestBus, ServerResponseConnector responseConnector,
                                     ServerNotificationConnector notificationConnector) {
        this.parent = parent;
        this.remoteRequestBus = remoteRequestBus;
        this.responseConnector = responseConnector;
        this.notificationConnector = notificationConnector;
        this.componentId = self().path().name();
        this.spawnerActor = context().actorOf(ClientSpawnerActor.props()
                        .withDispatcher("akka.actor.handshake-blocking-dispatcher"), componentId + "ClientSpawnerActor");
        this.waitingForClientInstance = false;
        FUSubscriptionClassifier classifier = new FUSubscriptionClassifier(componentId, "*");
        remoteRequestBus.subscribe(self(), classifier);
        //responseConnector.subscribe(self(), classifier);
        //We are the ones publishing the responses
        //notificationConnector.subscribe(self(), classifier);  //We are the ones publishing state updates
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                //Messages related to spawning client
                .match(WiringRequest.class, req -> createNewClientInstance(req.getInfo()))
                .match(ClientSpawnerMessages.ClientCreationFailed.class, msg -> notifyAboutClientCreationFailure())
                .match(ClientSpawnerMessages.ClientCreated.class, msg -> setClientAndAddObserver(msg))
                .match(ClientSpawnerMessages.ClientCreationCancelled.class, msg -> {/*Should we handle this here?*/})
                //Messages related to handshake
                .match(SubscribeToUpdatesRequest.class, req -> subscribeToRemoteState())
                .match(UnsubscribeToUpdatesRequest.class, req -> unsubscribeFromRemoteState())
                .match(InitiateHandoverRequest.class, req -> initRemoteHandshake())
                .match(StartHandoverRequest.class, req -> startRemoteHandshake())
                .build();
    }

    private void createNewClientInstance(WiringInfo wiringInfo) {
        log.info("Creating new client instance for endpoint " + wiringInfo.getRemoteEndpointURL() + " using wiringInfo " + wiringInfo);
        this.wiringInfo = wiringInfo;
        if (remoteNodeIds == null || waitingForClientInstance) {
            cancelClientCreationIfNecessaryAndCreateNew(wiringInfo);
        } else {
            log.info("Removing existing client and creating new one for endpoint " + wiringInfo.getRemoteEndpointURL());
            remoteNodeIds.getClient().removeSubscriber(this);
            remoteNodeIds.getClient().disconnect().thenAcceptAsync(c -> {
                remoteNodeIds.setClient(null);
                cancelClientCreationIfNecessaryAndCreateNew(wiringInfo);
            });
        }
    }

    private void cancelClientCreationIfNecessaryAndCreateNew(WiringInfo wiringInfo) {
        if (remoteNodeIds != null) {
            log.info("Cancelling client creation for new client with endpoint " + wiringInfo.getRemoteEndpointURL());
            spawnerActor.tell(new ClientSpawnerMessages.CancelClientCreation(), self());
        }
        log.info("Requesting new Client for endpoint " + wiringInfo.getRemoteEndpointURL());
        this.spawnerActor.tell(new ClientSpawnerMessages.CreateNewClient(wiringInfo), self());
        waitingForClientInstance = true;
    }

    private void setClientAndAddObserver(ClientSpawnerMessages.ClientCreated message) {
        log.info("Client created successfully");
        waitingForClientInstance = false;
        remoteNodeIds = message.getServerNodeIds();
        parent.tell(message.getServerNodeIds(), self());   //Let parent know that client is ready
        remoteNodeIds.getClient().addSubscriber(this);
    }

    private void subscribeToRemoteState() {
        log.info("Subscribing to remote state variable");
        remoteNodeIds.getClient().subscribeToStatus(remoteNodeIds.getStateVar());
    }

    private void unsubscribeFromRemoteState() {
        log.info("Unsubscribing from remote state variable");
        if (remoteNodeIds != null) {    //We need to check here, since wiring may fail
            remoteNodeIds.getClient().unsubscribeFromStatus();
        }
    }

    private void initRemoteHandshake() throws Exception {
        log.info("Initializing OpcUa handshake");
        remoteNodeIds.getClient().callStringMethod(remoteNodeIds.getInitMethod())
                .exceptionally(ex -> {
                    log.warning("Exception Calling Init Method on OPCUA Node: " +
                            remoteNodeIds.getInitMethod().toParseableString() + ex.getMessage());
                    ex.printStackTrace();
                    responseConnector.publish(new ServerHandshakeResponseEvent(componentId,
                            HandshakeCapability.ServerMessageTypes.NotOkResponseInitHandover));
                    return HandshakeCapability.ServerMessageTypes.NotOkResponseInitHandover.toString();
                })
                .thenAccept(result -> {
                    log.info("Remote server init handshake response: " + (result.equals("") ? "Ok" : result));
                    if (result.equals("")) {     //Workaround since 4diac version does not return anything
                        responseConnector.publish(new ServerHandshakeResponseEvent(componentId,
                                HandshakeCapability.ServerMessageTypes.OkResponseInitHandover));
                    } else {
                        responseConnector.publish(new ServerHandshakeResponseEvent(componentId,
                                HandshakeCapability.ServerMessageTypes.valueOf(result)));
                    }
                });
    }

    private void startRemoteHandshake() throws Exception {
        log.info("Starting OpcUa handshake");
        remoteNodeIds.getClient().callStringMethod(remoteNodeIds.getStartMethod())
                .exceptionally(ex -> {
                    log.warning("Exception Calling Start Method on OPCUA Node: " +
                            remoteNodeIds.getStartMethod().toParseableString() + ex.getMessage());
                    ex.printStackTrace();
                    responseConnector.publish(new ServerHandshakeResponseEvent(componentId,
                            HandshakeCapability.ServerMessageTypes.NotOkResponseStartHandover));
                    return HandshakeCapability.ServerMessageTypes.NotOkResponseStartHandover.toString();
                })
                .thenAccept(result -> {
                    log.info("Remote server start handshake response: " + (result.equals("") ? "Ok" : result));
                    if (result.equals("")) {     //Workaround since 4diac version does not return anything
                        responseConnector.publish(new ServerHandshakeResponseEvent(componentId,
                                HandshakeCapability.ServerMessageTypes.OkResponseStartHandover));
                    } else {
                        responseConnector.publish(new ServerHandshakeResponseEvent(componentId,
                                HandshakeCapability.ServerMessageTypes.valueOf(result)));
                    }
                });
    }

    public void notifyAboutClientCreationFailure() {
        log.info("Client creation failed");
        parent.tell(new ClientSpawnerMessages.ClientCreationFailed(), self());
    }

    @Override
    public void notifyAboutStateChange(Object currentState) {
        ServerHandshakeStatusUpdateEvent statusUpdateEvent;
        statusUpdateEvent = new ServerHandshakeStatusUpdateEvent(componentId, ServerSideStates.valueOf(currentState.toString()));
        log.info("Received subscription value update: " + statusUpdateEvent);
        notificationConnector.publish(statusUpdateEvent);
    }
}
