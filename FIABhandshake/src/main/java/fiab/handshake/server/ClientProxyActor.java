package fiab.handshake.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.core.capabilities.functionalunit.ResetRequest;
import fiab.core.capabilities.handshake.server.InitiateHandoverRequest;
import fiab.core.capabilities.handshake.server.StartHandoverRequest;
import fiab.functionalunit.connector.FUConnector;
import fiab.functionalunit.connector.FUSubscriptionClassifier;
import fiab.handshake.connector.ServerResponseConnector;
import fiab.handshake.server.messages.ServerHandshakeResponseEvent;

/**
 * This actor is responsible for notifying client about init/start response.
 * It is not possible to get an event from the eventbus without being a subscriber, hence this actor
 */
public class ClientProxyActor extends AbstractActor {

    public static Props props(FUConnector requestConnector, ServerResponseConnector responseConnector){
        return Props.create(ClientProxyActor.class, () -> new ClientProxyActor(requestConnector, responseConnector));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final FUConnector requestConnector;
    private final ServerResponseConnector responseConnector;
    private ActorRef sender;

    ClientProxyActor(FUConnector requestConnector, ServerResponseConnector responseConnector){
        this.requestConnector = requestConnector;
        this.responseConnector = responseConnector;
        FUSubscriptionClassifier subscriptionClassifier = new FUSubscriptionClassifier(self().path().name(), "*");
        responseConnector.subscribe(self(), subscriptionClassifier);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(InitiateHandoverRequest.class, msg -> {
                    this.sender = sender();
                    requestConnector.publish(msg);
                })
                .match(StartHandoverRequest.class, msg ->{
                    this.sender = sender();
                    requestConnector.publish(msg);
                })
                .match(ServerHandshakeResponseEvent.class, msg -> {
                    if(sender != null) {    //This may be the case when we initialize the handshake locally
                        this.sender.tell(msg, self());
                    }
                })
                .matchAny(msg -> log.warning("Received unsupported message: " + msg))
                .build();
    }
}
