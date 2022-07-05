package example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import client.ClientNode;
import client.ROSClient;
import example.actor.ROSClientActor;
import example.msg.AkkaAddRequest;
import org.ros.exception.ServiceNotFoundException;
import rosjava_test_msgs.AddTwoInts;

public class ROSTestApplication {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        ROSClient rosClient = ROSClient.newInstance(ClientNode.class);

        try {
            rosClient.createServiceClient("AddTwoInts", AddTwoInts._TYPE);
            ActorRef actor = system.actorOf(ROSClientActor.props(null, rosClient));
            actor.tell(new AkkaAddRequest(2, 3), ActorRef.noSender());
        } catch (ServiceNotFoundException e) {
            e.printStackTrace();
        }
    }
}
