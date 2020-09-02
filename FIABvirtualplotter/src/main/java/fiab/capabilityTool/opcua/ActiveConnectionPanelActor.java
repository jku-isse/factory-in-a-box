package fiab.capabilityTool.gui;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import fiab.capabilityTool.gui.msg.*;
import fiab.capabilityTool.opcua.CapabilityManagerClient;

public class ActiveConnectionPanelActor extends AbstractActor {

    private final ActorRef client;

    public static Props props(String url) {
        return Props.create(ActiveConnectionPanelActor.class, url);
    }

    public ActiveConnectionPanelActor(String url) {
        this.client = context().actorOf(CapabilityManagerClient.props(url));
    }

    @Override
    public Receive createReceive() {
        return new ReceiveBuilder()
                .match(ClientReadyNotification.class, notification -> {
                    getSender().tell(new ReadRequest(), self());
                })
                .match(ReadNotification.class, notification -> {
                    getContext().getParent().tell(notification, self());
                })
                .match(WriteRequest.class, request -> {
                    client.tell(request, self());
                })
                .build();
    }
}
