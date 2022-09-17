import example.actor.MessageServiceActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import client.ClientNode;
import client.ROSClient;
import example.msg.ROSCallbackNotification;
import example.msg.akka.AkkaEjectRequest;
import example.msg.akka.AkkaResetRequest;
import example.server.ROSServer;
import example.server.ServerNode;
import internal.FIABNodeConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ros.exception.ServiceNotFoundException;
import org.ros.node.NodeConfiguration;
import ros_basic_machine_msg.ResetService;
import ros_io_msg.EjectService;
import rosjava_test_msgs.AddTwoInts;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Tag("IntegrationTest")
public class RosServerClientTest {

    public static final String ROS_MASTER_IP_RPI = "192.168.133.118";
    public static final String ROS_MASTER_IP_LOCAL = "192.168.133.88";

    @Test
    public void testServerAndClient() {
        ActorSystem system = ActorSystem.create();

        TestKit probe = new TestKit(system);
        //This is a robot endpoint in theory
        ROSServer server = ROSServer.newInstanceWithMaster(ServerNode.class, ROSServer.DEFAULT_PORT);
        system.scheduler().scheduleOnce(Duration.ofSeconds(3), () -> {  //FIXME server startup is async. Remove waiting
            //ROSClient rosClient = ROSClient.newInstance(ClientNode.class);
            assertDoesNotThrow(() -> {

                NodeConfiguration nodeConfiguration = FIABNodeConfig.createNodeConfiguration("127.0.0.1",
                        "TestNodeId", new URI("http://" + ROS_MASTER_IP_LOCAL + ":11311"));
                ROSClient rosClient = ROSClient.newInstance(ClientNode.class, nodeConfiguration);
                //Call this for each messsage type you want to support
                //e.g.  rosClient.createServiceClient("TurnToPos", TurnToPos._TYPE);
                rosClient.createServiceClient("FIAB_reset_service", ResetService._TYPE);
                rosClient.createServiceClient("FIAB_eject_service", EjectService._TYPE);

                //Create an actor, since we have instantiated a new client and created a service client for each msg type
                ActorRef actor = system.actorOf(MessageServiceActor.props(probe.getRef(), rosClient));
                //Tell the actor to reset
                actor.tell(new AkkaResetRequest(), probe.getRef());
                //Wait for the actor to give us the result after the ros call has been successful
                ROSCallbackNotification result = probe.expectMsgClass(ROSCallbackNotification.class);
                //Check if the result is correct
                assertTrue(result.getOk());

                //Tell the actor to reset
                actor.tell(new AkkaEjectRequest(), probe.getRef());
                //Wait for the actor to give us the result after the ros call has been successful
                result = probe.expectMsgClass(ROSCallbackNotification.class);
                //Check if the result is correct
                assertTrue(result.getOk());
            });
        }, system.dispatcher());
    }
}
