package example.server;

import internal.node.FIABAbstractNodeMain;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseBuilder;
import rosjava_test_msgs.AddTwoInts;
import rosjava_test_msgs.AddTwoIntsRequest;
import rosjava_test_msgs.AddTwoIntsResponse;

public class ServerNode extends FIABAbstractNodeMain {

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("fiab_ros/server");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        //Register a new service
        connectedNode.newServiceServer("AddTwoInts", AddTwoInts._TYPE,
                new ServiceResponseBuilder<AddTwoIntsRequest, AddTwoIntsResponse>() {
                    @Override
                    public void
                    build(AddTwoIntsRequest request, AddTwoIntsResponse response) {
                        response.setSum(request.getA() + request.getB());
                    }
                });
    }
}
