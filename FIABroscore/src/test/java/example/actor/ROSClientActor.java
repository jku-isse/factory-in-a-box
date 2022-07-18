package example.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import client.ROSClient;
import example.msg.AkkaAddRequest;
import example.msg.SumNotification;
import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import ros_basic_machine_msg.ResetService;
import ros_basic_machine_msg.ResetServiceRequest;
import ros_basic_machine_msg.ResetServiceResponse;
import ros_io_msg.EjectService;
import ros_io_msg.EjectServiceRequest;
import ros_io_msg.EjectServiceResponse;

/**
 * Example implementation of a ROS client actor
 */
public class ROSClientActor extends AbstractActor {


    public static Props props(ActorRef parent, ROSClient rosClient) {
        return Props.create(MessageServiceActor.class, () -> new MessageServiceActor(parent, rosClient));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorRef parent;
    private final ROSClient rosClient;

    ROSClientActor(ActorRef parent, ROSClient rosClient) {
        this.parent = parent;
        this.rosClient = rosClient;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                //If we receive an add request, call the sendTwoIntsRequest to perform a ROS call
                .match(AkkaAddRequest.class, req -> sendTwoIntsRequest(req.getA(), req.getB()))
                //If we receive a local callback from ROS, we are done and can publish the result
                .match(LocalRosSumCallBackNotification.class, msg -> publishResult(msg))
                //All other messages are not supported, print a warning
                .matchAny(msg -> log.warning("Cannot process message: " + msg))
                .build();
    }

    private void sendTwoIntsRequest(int a, int b) {
        //Retrieve an existing serviceClient from the ROSClient wrapper using the type as id
        ServiceClient<ResetServiceRequest, ResetServiceResponse> serviceClient;
        serviceClient = (ServiceClient<ResetServiceRequest, ResetServiceResponse>) rosClient.getServiceClient(ResetService._TYPE);
        //call the new request using our local msg factory and attach a responseListener
        serviceClient.call(createNewResetServiceRequest(), new ServiceResponseListener<ResetServiceResponse>() {
            //In case ROS called the service successfully
            @Override
            public void onSuccess(ResetServiceResponse response) {
                log.debug("Success handler has been called");
                //Tell yourself that the ROS call has succeeded.
                self().tell(new LocalRosSumCallBackNotification(Integer.valueOf(String.valueOf(response.getSuccess()))), self());
            }
            //In case ROS failed to call the service
            @Override
            public void onFailure(RemoteException e) {
                log.error("Error handler has been called");
            }
        });
    }

    /**
     * Tells the parent the sum via a message
     * @param notification
     */
    private void publishResult(LocalRosSumCallBackNotification notification) {
        log.debug("Publishing sum " + notification.getSum());
        if (parent != null) {
            parent.tell(new SumNotification(notification.getSum()), self());
        }
    }

    /**
     *
     * Factory for convenient creation of messages

     */
    private ResetServiceRequest createNewResetServiceRequest() {
        return rosClient.createNewMessage(ResetService._TYPE, ResetServiceRequest.class);
    }
}
