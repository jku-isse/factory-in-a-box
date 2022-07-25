package example.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import client.ROSClient;
import example.msg.*;
import example.msg.akka.AkkaEjectDoneNotification;
import example.msg.akka.AkkaEjectRequest;
import example.msg.akka.AkkaResetDoneNotification;
import example.msg.akka.AkkaResetRequest;
import example.msg.ros.EjectRequest;
import example.msg.ros.ResetRequest;
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

    /**
     * The usual props factory
     * @param parent who will this actor respond to
     * @param rosClient a fully initialized client
     * @return props for the MessageServiceActor
     */
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
                .match(AkkaEjectRequest.class, req -> sendEjectRequest())
                .match(AkkaResetRequest.class, req -> sendResetRequest())
                //If we receive a local callback from ROS, we are done and can publish the result
                .match(AkkaResetDoneNotification.class, msg -> publishResult(msg.getSuccess()))
                .match(AkkaEjectDoneNotification.class, msg -> publishResult(msg.getSuccess()))
                //All other messages are not supported, print a warning
                .matchAny(msg -> log.warning("Cannot process message: " + msg))
                .build();
    }

    private void sendEjectRequest() {
        //Retrieve an existing serviceClient from the ROSClient wrapper using the type as id
        ServiceClient<EjectServiceRequest, EjectServiceResponse> serviceClient;
        serviceClient = rosClient.getServiceClient(EjectService._TYPE);
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
        ServiceClient<ResetServiceRequest, ResetServiceResponse> serviceClient = rosClient.getServiceClient(ResetService._TYPE);
        //call the new request using our local msg factory and attach a responseListener
        serviceClient.call(createResetServiceRequest(), new ServiceResponseListener<>() {
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
    private void publishResult(boolean success) {
        log.debug("ROS call successful? " + success);
        if (parent != null) {
            parent.tell(new ROSCallbackNotification(success), self());
        }
    }

    /**
     * Factory for convenient creation of messages
     */
    private EjectServiceRequest createEjectServiceRequest() {
        return rosClient.createNewMessage(EjectService._TYPE, EjectServiceRequest.class);
    }

    /**
     * Factory for convenient creation of messages
     */
    private ResetServiceRequest createResetServiceRequest() {
        return rosClient.createNewMessage(ResetService._TYPE, ResetServiceRequest.class);
    }

    /**
     * Factory for convenient creation of messages
     */
    private StopServiceRequest createStopServiceRequest() {
        return rosClient.createNewMessage(StopService._TYPE, StopServiceRequest.class);
    }
}

