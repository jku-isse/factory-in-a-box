package fiab.iostation.opcua;

import akka.actor.Props;
import client.ClientNode;
import client.ROSClient;
import fiab.core.capabilities.handshake.HandshakeCapability;
import fiab.core.capabilities.handshake.server.TransportAreaStatusOverrideRequest;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.handshake.server.statemachine.ServerSideHandshakeTriggers;
import fiab.opcua.server.OPCUABase;
import internal.FIABNodeConfig;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.ros.exception.RemoteException;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import ros_basic_machine_msg.*;
import ros_io_msg.EjectService;
import ros_io_msg.EjectServiceRequest;
import ros_io_msg.EjectServiceResponse;

import java.net.URI;
//import rosjava_custom_srv.MessageService;

public class OpcUaInputStationActorROS extends OpcUaInputStationActor {

    protected ROSClient rosClient;

    public static Props props(ROSClient rosClient, OPCUABase base, UaFolderNode rootNode, MachineEventBus eventBus) {
        return Props.create(OpcUaInputStationActorROS.class, () -> new OpcUaInputStationActorROS(rosClient, base, rootNode, eventBus));
    }

    public OpcUaInputStationActorROS(ROSClient rosClient, OPCUABase base, UaFolderNode root, MachineEventBus eventBus) {
        super(base, root, eventBus);
        this.rosClient = rosClient;
    }

    @Override
    public void doResetting() {
        //Simulate behaviour of sensor waiting for pallet -> Auto reload to loaded
        self().tell(new TransportAreaStatusOverrideRequest(componentId, HandshakeCapability.StateOverrideRequests.SetLoaded), self());
        ServiceClient<ResetServiceRequest, ResetServiceResponse> serviceClient;
        serviceClient = rosClient.getServiceClient(ResetService._TYPE);
        //call the new request using our local msg factory and attach a responseListener
        serviceClient.call(createResetServiceRequest(), new ServiceResponseListener<ResetServiceResponse>() {
            //In case ROS called the service successfully
            @Override
            public void onSuccess(ResetServiceResponse response) {
                log.debug("Received reset response via ROS");
                //Tell yourself that the ROS call has succeeded.
                stateMachine.fireIfPossible(ServerSideHandshakeTriggers.RESETTING_DONE);
                //self().tell(new ROSCallbackNotification(response.getSuccess()), self());
            }
            //In case ROS failed to call the service
            @Override
            public void onFailure(RemoteException e) {
                log.error("Error handler has been called");
                stateMachine.fireIfPossible(ServerSideHandshakeTriggers.STOP);
            }
        });
    }

    @Override
    public void doStopping() {
        //TODO
        //on success -> ServerSideHandshakeTriggers.STOPPING_DONE
        //on fail -> ServerSideHandshakeTriggers.STOPPING_DONE  //FOR NOW
        //Simulate behaviour of sensor waiting for pallet -> Auto reload to loaded
        ServiceClient<StopServiceRequest, StopServiceResponse> serviceClient;
        serviceClient = rosClient.getServiceClient(StopService._TYPE);
        //call the new request using our local msg factory and attach a responseListener
        serviceClient.call(createStopServiceRequest(), new ServiceResponseListener<StopServiceResponse>() {
            //In case ROS called the service successfully
            @Override
            public void onSuccess(StopServiceResponse response) {
                log.debug("Received reset response via ROS");
                //Tell yourself that the ROS call has succeeded.
                stateMachine.fireIfPossible(ServerSideHandshakeTriggers.STOPPING_DONE);
                //self().tell(new ROSCallbackNotification(response.getSuccess()), self());
            }
            //In case ROS failed to call the service
            @Override
            public void onFailure(RemoteException e) {
                log.error("Error handler has been called");
                stateMachine.fireIfPossible(ServerSideHandshakeTriggers.STOP);
            }
        });
    }

    @Override
    public void doExecute() {
        //TODO
        //on success -> ServerSideHandshakeTriggers.COMPLETE
        //on fail -> ServerSideHandshakeTriggers.STOP
        ServiceClient<EjectServiceRequest, EjectServiceResponse> serviceClient;
        serviceClient = rosClient.getServiceClient(EjectService._TYPE);
        //call the new request using our local msg factory and attach a responseListener
        serviceClient.call(createEjectServiceRequest(), new ServiceResponseListener<EjectServiceResponse>() {
            //In case ROS called the service successfully
            @Override
            public void onSuccess(EjectServiceResponse response) {
                log.debug("Received reset response via ROS");
                //Tell yourself that the ROS call has succeeded.
                stateMachine.fireIfPossible(ServerSideHandshakeTriggers.COMPLETE);
                //self().tell(new ROSCallbackNotification(response.getSuccess()), self());
            }
            //In case ROS failed to call the service
            @Override
            public void onFailure(RemoteException e) {
                log.error("Error handler has been called");
                stateMachine.fireIfPossible(ServerSideHandshakeTriggers.STOP);
            }
        });
    }



    /**
     * Factory for convenient creation of messages
     */
    private ResetServiceRequest createResetServiceRequest() {
        return rosClient.createNewMessage(ResetService._TYPE, ResetServiceRequest.class);
    }


    private EjectServiceRequest createEjectServiceRequest() {
        EjectServiceRequest request = rosClient.createNewMessage(EjectService._TYPE, EjectServiceRequest.class);
        return request;
    }


    private StopServiceRequest createStopServiceRequest(){
        StopServiceRequest request = rosClient.createNewMessage(StopService._TYPE, StopServiceRequest.class);
        return request;
    }
}



