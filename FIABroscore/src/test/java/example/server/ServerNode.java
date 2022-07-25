package example.server;

import internal.node.FIABAbstractNodeMain;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseBuilder;

import ros_basic_machine_msg.ResetService;
import ros_basic_machine_msg.ResetServiceRequest;
import ros_basic_machine_msg.ResetServiceResponse;
import ros_io_msg.EjectService;
import ros_io_msg.EjectServiceRequest;
import ros_io_msg.EjectServiceResponse;
import rosjava_test_msgs.AddTwoInts;
import rosjava_test_msgs.AddTwoIntsRequest;
import rosjava_test_msgs.AddTwoIntsResponse;

/**
 * A ServerNode that can be registered as a ROS Node
 */
public class ServerNode extends FIABAbstractNodeMain {

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("fiab_ros/server");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        //Register a new service
        connectedNode.newServiceServer("FIAB_reset_service", ResetService._TYPE,
                new ServiceResponseBuilder<ResetServiceRequest, ResetServiceResponse>() {
                    @Override
                    public void
                    build(ResetServiceRequest request, ResetServiceResponse response) {
                        //Since we do nothing here, we just set the success flag to true
                        //The method sends the response when the body of this method has finished executing
                        response.setSuccess(true);
                    }
                });
        //We can in the same way register many more services, like in the following example
        connectedNode.newServiceServer("FIAB_eject_service", EjectService._TYPE,
                new ServiceResponseBuilder<EjectServiceRequest, EjectServiceResponse>() {
                    @Override
                    public void
                    build(EjectServiceRequest request, EjectServiceResponse response) {
                        //Since we do nothing here, we just set the success flag to true
                        //The method sends the response when the body of this method has finished executing
                        response.setSuccess(true);
                    }
                });
    }
}
