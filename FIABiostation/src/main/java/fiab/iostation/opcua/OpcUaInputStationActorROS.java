package fiab.iostation.opcua;

import akka.actor.Props;
import fiab.functionalunit.connector.MachineEventBus;
import fiab.opcua.server.OPCUABase;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import rosjava_custom_srv.MessageService;

public class OpcUaInputStationActorROS extends OpcUaInputStationActor{


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("OpcUaInputStationActor/server");
    }


    public static Props props(OPCUABase base, UaFolderNode rootNode, MachineEventBus eventBus) {
        return Props.create(OpcUaInputStationActorROS.class, () -> new OpcUaInputStationActorROS(base, rootNode, eventBus));
    }

    public OpcUaInputStationActorROS(OPCUABase base, UaFolderNode root, MachineEventBus eventBus) {
        super(base, root, eventBus);
    }

    @Override
    public void doResetting() {
        super.doResetting();   //Wait for the sensor to detect a pallet
        //Simulate behaviour of sensor waiting for pallet -> Auto reload to loaded
        //TODO

    }

    @Override
    public void doExecute() {
        //super.doExecute();    //We provide our own implementation here
        //self().tell(new TransportAreaStatusOverrideRequest(componentId, HandshakeCapability.StateOverrideRequests.SetEmpty), self());
        //For now we just simulate it, later we can use real hardware e.g. via ROS for example
        //TODO call ROS service
        //Do this when done
        //self().tell(new CompleteHandshake(componentId), self());
        ConnectedNode connectedNode = null;
        assert false;
//        connectedNode.newServiceServer("add_two_ints", AddTwoInts._TYPE,
//                new ServiceResponseBuilder<AddTwoIntsRequest, AddTwoIntsResponse>() {
//                    public void
//                    build(AddTwoIntsRequest request,AddTwoIntsResponse response) {response.setSum(request.getA() + request.getB());
//                        System.out.println("Received value is :" + response.getSum());
//                    }
//                });
//    }
//}
