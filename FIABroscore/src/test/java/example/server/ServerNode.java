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
                        response.setSuccess(true);
//                        response.setSum(request.getA() + request.getB());
                    }
                });
        connectedNode.newServiceServer("FIAB_eject_service", EjectService._TYPE,
                new ServiceResponseBuilder<EjectServiceRequest, EjectServiceResponse>() {
                    @Override
                    public void
                    build(EjectServiceRequest request, EjectServiceResponse response) {
                        response.setSuccess(true);
//                        response.setSum(request.getA() + request.getB());
                    }
                });
    }
}
