package internal.node;

import akka.actor.ActorRef;
import org.ros.internal.message.Message;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;

public abstract class FIABAbstractNodeMain implements FIABNodeMain {

    @Override
    public void callRosService(Message request, ActorRef actor) {

    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

    }

    @Override
    public void onShutdown(Node node) {

    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }
}
