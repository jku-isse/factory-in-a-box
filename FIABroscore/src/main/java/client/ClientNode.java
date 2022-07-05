package client;

import akka.actor.ActorRef;
import internal.node.FIABAbstractNodeMain;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import rosjava_test_msgs.AddTwoIntsRequest;
import rosjava_test_msgs.AddTwoIntsResponse;

public class ClientNode extends FIABAbstractNodeMain {

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("fiab_ros/client");
    }

}
