package fiab.mes.assembly.display.msg;

import akka.actor.ActorRef;

public class DisplayRequest {

    private final ActorRef sender;


    public DisplayRequest(ActorRef sender) {
        this.sender = sender;
    }

    public ActorRef getSender() {
        return sender;
    }
}
