package example.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import client.ROSClient;
import example.msg.*;
import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;


import ros_basic_machine_msg.*;
import ros_io_msg.EjectService;
import ros_io_msg.EjectServiceRequest;
import ros_io_msg.EjectServiceResponse;


/**
 * Example implementation of a ROS client actor
 */
public class MessageServiceActor extends AbstractActor {


    public static Props props(ActorRef parent, ROSClient rosClient) {
        return Props.create(MessageServiceActor.class, () -> new MessageServiceActor(parent, rosClient));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorRef parent;
    private final ROSClient rosClient;

    MessageServiceActor(ActorRef parent, ROSClient rosClient) {
        this.parent = parent;
        this.rosClient = rosClient;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                //If we receive an add request, call the sendTwoIntsRequest to perform a ROS call
                .match(EjectRequest.class, req -> sendEjectRequest())
                .match(ResetRequest.class, req -> sendResetRequest())
                //If we receive a local callback from ROS, we are done and can publish the result
                //.match(LocalRosSumCallBackNotification.class, msg -> publishResult(msg))
                .match(ROSCallbackNotification.class, req -> log.info("Received {}", req.getOk()))
                //All other messages are not supported, print a warning
                .matchAny(msg -> log.warning("Cannot process message: " + msg))
                .build();
    }

    private void sendEjectRequest() {
        //Retrieve an existing serviceClient from the ROSClient wrapper using the type as id
        ServiceClient<EjectServiceRequest, EjectServiceResponse> serviceClient;
        serviceClient = (ServiceClient<EjectServiceRequest, EjectServiceResponse>) rosClient.getServiceClient(EjectService._TYPE);
        //call the new request using our local msg factory and attach a responseListener
        serviceClient.call(createEjectServiceRequest(), new ServiceResponseListener<EjectServiceResponse>() {
            //In case ROS called the service successfully
            @Override
            public void onSuccess(EjectServiceResponse response) {
                log.debug("Success handler has been called");
                //Tell yourself that the ROS call has succeeded.
                self().tell(new ROSCallbackNotification(true), self());
            }
            //In case ROS failed to call the service
            @Override
            public void onFailure(RemoteException e) {
                log.error("Error handler has been called");
            }
        });
    }

    private void sendResetRequest() {
        //Retrieve an existing serviceClient from the ROSClient wrapper using the type as id
        ServiceClient<ResetServiceRequest, ResetServiceResponse> serviceClient;
        serviceClient = (ServiceClient<ResetServiceRequest, ResetServiceResponse>) rosClient.getServiceClient(ResetService._TYPE);
        //call the new request using our local msg factory and attach a responseListener
        serviceClient.call(createResetServiceRequest(), new ServiceResponseListener<ResetServiceResponse>() {
            //In case ROS called the service successfully
            @Override
            public void onSuccess(ResetServiceResponse response) {
                log.debug("Success handler has been called");
                //Tell yourself that the ROS call has succeeded.
                self().tell(new ROSCallbackNotification(response.getSuccess()), self());
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
     */
    private void publishResult(LocalRosSumCallBackNotification notification) {
        log.debug("Publishing sum " + notification.getSum());
        if (parent != null) {
            parent.tell(new SumNotification(notification.getSum()), self());
        }
    }

    /**
     * Factory for convenient creation of messages
     */
    private EjectServiceRequest createEjectServiceRequest() {
        EjectServiceRequest request = rosClient.createNewMessage(EjectService._TYPE, EjectServiceRequest.class);
        return request;
    }

    /**
     * Factory for convenient creation of messages
     */
    private ResetServiceRequest createResetServiceRequest() {
        ResetServiceRequest request = rosClient.createNewMessage(ResetService._TYPE, ResetServiceRequest.class);
        return request;
    }

    /**
     * Factory for convenient creation of messages
     */

    private StopServiceRequest createStopServiceRequest() {
        StopServiceRequest request = rosClient.createNewMessage(StopService._TYPE, StopServiceRequest.class);
        return request;
    }
}

