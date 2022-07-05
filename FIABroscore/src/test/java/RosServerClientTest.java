import example.actor.ROSClientActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import client.ClientNode;
import client.ROSClient;
import example.msg.AkkaAddRequest;
import example.msg.SumNotification;
import org.junit.jupiter.api.Test;
import org.ros.exception.ServiceNotFoundException;
import rosjava_test_msgs.AddTwoInts;
import example.server.ROSServer;
import example.server.ServerNode;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RosServerClientTest {

    @Test
    public void testServerAndClient() {
        ActorSystem system = ActorSystem.create();

        TestKit probe = new TestKit(system);
        //This is a robot endpoint in theory
        ROSServer server = ROSServer.newInstanceWithMaster(ServerNode.class, ROSServer.DEFAULT_PORT);
        system.scheduler().scheduleOnce(Duration.ofSeconds(3), () -> {  //FIXME
            ROSClient rosClient = ROSClient.newInstance(ClientNode.class);

            assertDoesNotThrow(() -> {
                //Call this for each messsage type you want to support
                //e.g.  rosClient.createServiceClient("TurnToPos", TurnToPos._TYPE);
                rosClient.createServiceClient("AddTwoInts", AddTwoInts._TYPE);
            });

            //Create an actor, since we have instantiated a new client and created a service client for each msg type
            ActorRef actor = system.actorOf(ROSClientActor.props(probe.getRef(), rosClient));
            //Tell the actor to add two numbers
            actor.tell(new AkkaAddRequest(2, 2), probe.getRef());
            //Wait for the actor to give us the result after the ros call has been successful
            SumNotification result = probe.expectMsgClass(SumNotification.class);
            //Check if the result is correct
            assertEquals(4, result.getSum());
        }, system.dispatcher());
    }
}
