package fiab.iostation.opcua;

import akka.actor.Props;
import client.ClientNode;
import client.ROSClient;
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
import ros_basic_machine_msg.ResetService;
import ros_basic_machine_msg.ResetServiceRequest;
import ros_basic_machine_msg.ResetServiceResponse;
import ros_io_msg.EjectService;

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
    }

    @Override
    public void doExecute() {
        //TODO
        //on success -> ServerSideHandshakeTriggers.COMPLETE
        //on fail -> ServerSideHandshakeTriggers.STOP
    }

    /**
     * Factory for convenient creation of messages
     */
    private ResetServiceRequest createResetServiceRequest() {
        ResetServiceRequest request = rosClient.createNewMessage(ResetService._TYPE, ResetServiceRequest.class);
        return request;
    }
}
