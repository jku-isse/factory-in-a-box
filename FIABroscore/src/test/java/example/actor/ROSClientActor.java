package example.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import client.ROSClient;
import example.msg.ROSCallbackNotification;
import example.msg.akka.AkkaEjectDoneNotification;
import example.msg.akka.AkkaEjectRequest;
import example.msg.akka.AkkaResetDoneNotification;
import example.msg.akka.AkkaResetRequest;
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
                //If we receive a reset request, call the reset service via ros
                .match(AkkaResetRequest.class, req -> sendResetRequest())
                //If we receive a eject request, call the eject service via ros
                .match(AkkaEjectRequest.class, req -> sendEjectRequest())
                //If we receive a local callback from ROS, we are done and can publish the result
                .match(AkkaResetDoneNotification.class, msg -> publishResult(msg.getSuccess()))
                .match(AkkaEjectDoneNotification.class, msg -> publishResult(msg.getSuccess()))
                //All other messages are not supported, print a warning
                .matchAny(msg -> log.warning("Cannot process message: " + msg))
                .build();
    }

    private void sendResetRequest() {
        //Retrieve an existing serviceClient from the ROSClient wrapper using the type as id
        ServiceClient<ResetServiceRequest, ResetServiceResponse> serviceClient;
        serviceClient = (ServiceClient<ResetServiceRequest, ResetServiceResponse>) rosClient.getServiceClient(ResetService._TYPE);
        //call the new request using our local msg factory and attach a responseListener
        serviceClient.call(createNewResetServiceRequest(), new ServiceResponseListener<ResetServiceResponse>() {
            //In case ROS called the service successfully
            @Override
            public void onSuccess(ResetServiceResponse response) {
                log.debug("Reset success handler has been called");
                //Tell yourself that the ROS call has succeeded.
                self().tell(new AkkaResetDoneNotification((response.getSuccess())), self());
            }
            //In case ROS failed to call the service
            @Override
            public void onFailure(RemoteException e) {
                log.error("Reset error handler has been called");
            }
        });
    }

    private void sendEjectRequest() {
        //Retrieve an existing serviceClient from the ROSClient wrapper using the type as id
        ServiceClient<EjectServiceRequest, EjectServiceResponse> serviceClient;
        serviceClient = (ServiceClient<EjectServiceRequest, EjectServiceResponse>) rosClient.getServiceClient(EjectService._TYPE);
        //call the new request using our local msg factory and attach a responseListener
        serviceClient.call(createNewEjectServiceRequest(), new ServiceResponseListener<EjectServiceResponse>() {
            //In case ROS called the service successfully
            @Override
            public void onSuccess(EjectServiceResponse response) {
                log.debug("Eject success handler has been called");
                //Tell yourself that the ROS call has succeeded.
                self().tell(new AkkaEjectDoneNotification((response.getSuccess())), self());
            }
            //In case ROS failed to call the service
            @Override
            public void onFailure(RemoteException e) {
                log.error("Eject error handler has been called");
            }
        });
    }

    /**
     * Tells the parent the sum via a message
     * @param success
     */
    private void publishResult(boolean success) {
        log.debug("ROS call was successful? {}" + success);
        if (parent != null) {
            parent.tell(new ROSCallbackNotification(success), self());
        }
    }

    /**
     * Factory for convenient creation of messages
     */
    private ResetServiceRequest createNewResetServiceRequest() {
        return rosClient.createNewMessage(ResetService._TYPE, ResetServiceRequest.class);
    }

    /**
     * Factory for convenient creation of messages
     */
    private EjectServiceRequest createNewEjectServiceRequest() {
        return rosClient.createNewMessage(EjectService._TYPE, EjectServiceRequest.class);
    }
}
