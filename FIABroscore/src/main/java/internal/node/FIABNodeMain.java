package internal.node;

import akka.actor.ActorRef;
import org.ros.internal.message.Message;
import org.ros.node.NodeMain;

public interface FIABNodeMain extends NodeMain {

    void callRosService(Message request, ActorRef actor);
}
