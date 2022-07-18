package example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import client.ClientNode;
import client.ROSClient;
import example.actor.MessageServiceActor;
import example.msg.AkkaAddRequest;
import example.msg.EjectRequest;
import example.msg.ResetRequest;
import internal.FIABNodeConfig;
import org.ros.exception.ServiceNotFoundException;
import org.ros.node.NodeConfiguration;
import ros_basic_machine_msg.ResetService;
import ros_io_msg.EjectService;

import java.net.URI;
import java.net.URISyntaxException;

public class ROSTestApplication {

    public static void main(String[] args) throws URISyntaxException {
        ActorSystem system = ActorSystem.create();
        {
            NodeConfiguration nodeConfiguration = FIABNodeConfig.createNodeConfiguration("192.168.133.88",
                    "TestNodeId", new URI("http://192.168.133.109:11311"));

            ROSClient rosClient = ROSClient.newInstance(ClientNode.class, nodeConfiguration);

            try {
                //rosClient.createServiceClient("AddTwoInts", AddTwoInts._TYPE);
                rosClient.createServiceClient("FIAB_reset_service", ResetService._TYPE);

                //rosClient.createServiceClient("FIAB_eject_service", EjectService._TYPE);
                ActorRef actor = system.actorOf(MessageServiceActor.props(null, rosClient));
                actor.tell(new ResetRequest(), ActorRef.noSender());
//                actor.tell(new EjectRequest(), ActorRef.noSender());
            } catch (ServiceNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
