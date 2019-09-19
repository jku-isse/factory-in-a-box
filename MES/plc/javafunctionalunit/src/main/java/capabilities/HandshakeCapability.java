/**
 * [Class description.  The first sentence should be a meaningful summary of the class since it
 * will be displayed as the class summary on the Javadoc package page.]
 * <p>
 * [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
 * about desired improvements, etc.]
 *
 * @author Michael Bishara
 * @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
 * @author <A HREF="https://github.com/michaelanis14">[Github: Michael Bishara]</A>
 * @date 4 Sep 2019
 **/
package capabilities;

import static helper.HandshakeStates.*;

import static helper.HandshakeStates.STOPPED;
import static helper.HandshakeStates.STOPPING;

import java.util.HashMap;
import java.util.Map;

import communication.Communication;
import communication.utils.RequestedNodePair;
import helper.CapabilityType;
import helper.CapabilityId;
import helper.HandshakeStates;
import helper.ClientLoadingStates;
import protocols.LoadingClientProtocol;
import protocols.LoadingServerProtocol;

public class HandshakeCapability  {
    Map<CapabilityId, CapabilityType> capabilityMap;
    Map<CapabilityId, String> wiringMap;
    public final static boolean DEBUG = true;
    // Map<CapabilityInstanceId, Protocol> protocolMap;
    LoadingClientProtocol clientProtocol;
    LoadingServerProtocol serverProtocol;
    int loadingMechanism;

    // opcua
    private Communication opcua_comm;
    private Object opcua_server;
    private Object parentObjectId;
	private Object opcua_object;

    private int unique_id;

    private int getUnique_id() {
        return unique_id += 1;
    }

    public void setWiring() {

        System.out.println("Method Callback stop");

    }

    public void initLoading() {

        System.out.println("Method Callback stop");

    }

    public void initUnloading() {

        System.out.println("Method Callback stop");

    }

    public HandshakeCapability(Communication opcua_comm, Object opcua_server, Object parentObjectId, int unique_id) {
       // super();
        clientProtocol = null;
        serverProtocol = null;
        capabilityMap = new HashMap<>();
        wiringMap = new HashMap<CapabilityId, String>();

        this.opcua_comm = opcua_comm;
        this.opcua_server = opcua_server;
        this.parentObjectId = parentObjectId;
        this.unique_id = unique_id;




		Object rewquestedHandshakeId = opcua_comm.getServerCommunication().createNodeNumeric(1, getUnique_id());
        opcua_object = opcua_comm.getServerCommunication().addNestedObject(opcua_server, parentObjectId, rewquestedHandshakeId, "Handshak FU");

		Object endpoint_reqId = opcua_comm.getServerCommunication().createNodeNumeric(1, getUnique_id());
		Object endpoint_object = opcua_comm.getServerCommunication().addNestedObject(opcua_server, opcua_object, endpoint_reqId, "END_POINT");

		Object requiredCapability_reqId = opcua_comm.getServerCommunication().createNodeNumeric(1, getUnique_id());
		Object requiredCapability_object = opcua_comm.getServerCommunication().addNestedObject(opcua_server, opcua_object, endpoint_object, "Required_Capability");

		Object endpoint2_reqId = opcua_comm.getServerCommunication().createNodeNumeric(1, getUnique_id());
		Object endpoint2_object = opcua_comm.getServerCommunication().addNestedObject(opcua_server, opcua_object, endpoint2_reqId, "END_POINT");

		Object providedCapability_reqId = opcua_comm.getServerCommunication().createNodeNumeric(1, getUnique_id());
		Object providedCapability_object = opcua_comm.getServerCommunication().addNestedObject(opcua_server, opcua_object, endpoint2_object, "Provided_Capability");


		Object west_reqId = opcua_comm.getServerCommunication().createNodeNumeric(1, getUnique_id());
		Object west_object = opcua_comm.getServerCommunication().addNestedObject(opcua_server, requiredCapability_object, west_reqId, "WEST");

		//should be moved to the base class
        opcua_comm.addStringMethodToServer(opcua_server, west_object, new RequestedNodePair<>(1, getUnique_id()), "Set_Wiring", x -> {
            setWiring();
            return "this.instanceId is now set to this.path";
        });

        opcua_comm.addStringMethodToServer(opcua_server, west_object, new RequestedNodePair<>(1, getUnique_id()), "initiate_Loading", x -> {
            initLoading();
            return "Loading was successful";
        });
        opcua_comm.addStringMethodToServer(opcua_server, west_object, new RequestedNodePair<>(1, getUnique_id()), "initiate_Unloading", x -> {
            initUnloading();
            return "unLoading was successful";
        });


        fireTrigger(IDLE);
    }

    static int currentState;
    LoadingClientProtocol EngageInUnLoading;

    public void fireTrigger(HandshakeStates states) {

        switch (states) {
            case IDLE:
                currentState = IDLE.ordinal();
                idle();
                break;
            case STARTING:
                currentState = STARTING.ordinal();
                starting();
                break;
            case EXECUTE:
                currentState = EXECUTE.ordinal();
                execute();
                break;
            case COMPLETING:
                currentState = COMPLETING.ordinal();
                completing();
                break;
            case COMPLETE:
                currentState = COMPLETE.ordinal();
                compelete();
                break;

            case RESETTING:
                currentState = RESETTING.ordinal();
                resetting();
                break;
            case STOPPING:
                currentState = STOPPING.ordinal();
                stopping();
                break;
            case STOPPED:
                currentState = STOPPED.ordinal();
                stopped();
                break;
            default:
                // DONE needs to be called from inside this class
                // Invalid input should not be handled
                break;
        }
    }

    public final int getCurrentState() {
        return currentState;
    }

    public void idle() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                opcua_comm.getServerCommunication().runServer(opcua_server);
            }
        }).start();
    }

    public void starting() {
        if (loadingMechanism == 1) {
            clientProtocol.fireTrigger(ClientLoadingStates.STARTING);
        }

    }

    public void execute() {

    }

    public void completing() {

    }

    public void compelete() {

    }

    public void resetting() {

    }

    public void stopping() {

    }

    public void stopped() {

    }

    public void initiateUnloading(String direction, String orderId) {
        // gageInUnLoading = new LoadingClientProtocol();
    }

    void initiateLoading(CapabilityId instanceId, String orderId) {
        loadingMechanism = 1;
        // this.protocolMap.get(instanceId)
        if (this.getCurrentState() != IDLE.ordinal()) {
            log("Not Idle - Wrong State. !");
            return;
        }

        clientProtocol.setServerPath(this.wiringMap.get(instanceId));
        clientProtocol.setOrderId(orderId);
        this.fireTrigger(STARTING);
    }

    public void setRequiredCapability(CapabilityId instanceId, CapabilityType typeId) {
        this.capabilityMap.put(instanceId, typeId);
        // this.protocolMap.put(instanceId, initCapability(instanceId));
        initCapability(instanceId);
    }

    public void setWiring(CapabilityId localCapabilityId, String remoteCapabiltyEntryPoint) { // Serveraddress+NodeID
        this.wiringMap.put(localCapabilityId, remoteCapabiltyEntryPoint);

    }

    public void initCapability(CapabilityId instanceId) {
        /*
         * } if (instanceId.toString().contains("SERVER")) return new
         * LoadingServerProtocol(); else return new LoadingClientProtocol();
         */
        if (instanceId.toString().contains("SERVER") && serverProtocol == null) {
            serverProtocol = new LoadingServerProtocol();

        } else if (clientProtocol == null) {
            clientProtocol = new LoadingClientProtocol();
        }
    }

    public static void log(String message) {
        if (DEBUG) {
            String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

            System.out.println(className + "." + methodName + "(): " + lineNumber + "  " + message);
        }
    }
}
