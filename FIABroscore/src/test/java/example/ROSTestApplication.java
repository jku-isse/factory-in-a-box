package example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import client.ClientNode;
import client.ROSClient;
import example.actor.MessageServiceActor;
import example.msg.akka.AkkaEjectRequest;
import example.msg.akka.AkkaResetRequest;
import example.msg.ros.EjectRequest;
import example.msg.ros.ResetRequest;
import internal.FIABNodeConfig;
import org.ros.exception.ServiceNotFoundException;
import org.ros.node.NodeConfiguration;
import ros_basic_machine_msg.ResetService;
import ros_io_msg.EjectService;

import java.net.URI;
import java.net.URISyntaxException;

public class ROSTestApplication {

    public static final String ROS_MASTER_IP_RPI = "192.168.133.109";
    public static final String ROS_MASTER_IP_LOCAL = "127.0.0.1";

    public static void main(String[] args) throws URISyntaxException {
        ActorSystem system = ActorSystem.create();
        {
            NodeConfiguration nodeConfiguration = FIABNodeConfig.createNodeConfiguration("127.0.0.1",
                    "TestNodeId", new URI("http://" + ROS_MASTER_IP_LOCAL + ":11311"));

            ROSClient rosClient = ROSClient.newInstance(ClientNode.class, nodeConfiguration);

            try {
                //rosClient.createServiceClient("AddTwoInts", AddTwoInts._TYPE);
                rosClient.createServiceClient("FIAB_reset_service", ResetService._TYPE);
                rosClient.createServiceClient("FIAB_eject_service", EjectService._TYPE);

                //rosClient.createServiceClient("FIAB_eject_service", EjectService._TYPE);
                ActorRef actor = system.actorOf(MessageServiceActor.props(null, rosClient));
                actor.tell(new AkkaResetRequest(), ActorRef.noSender());
                actor.tell(new AkkaEjectRequest(), ActorRef.noSender());
//                actor.tell(new EjectRequest(), ActorRef.noSender());
            } catch (ServiceNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
