/**
   [Class description.  The first sentence should be a meaningful summary of the class since it
   will be displayed as the class summary on the Javadoc package page.]

   [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
   about desired improvements, etc.]
   @author Michael Bishara
   @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
   @author <A HREF="https://github.com/michaelanis14">[Github: Michael Bishara]</A>
   @date 4 Sep 2019
**/
package functionalUnit;

import static helper.HandshakeStates.COMPLETE;
import static helper.HandshakeStates.COMPLETING;
import static helper.HandshakeStates.EXECUTE;
import static helper.HandshakeStates.IDLE;
import static helper.HandshakeStates.RESETTING;
import static helper.HandshakeStates.STARTING;
import static helper.HandshakeStates.STOPPED;
import static helper.HandshakeStates.STOPPING;

import java.util.HashMap;
import java.util.Map;

import communication.Communication;
import helper.CapabilityId;
import helper.CapabilityInstanceId;
import helper.ClientLoadingStates;
import helper.HandshakeStates;
import protocols.LoadingClientProtocol;
import protocols.LoadingServerProtocol;

public class HandshakeFunctionalUnit extends FunctionalUnit {
	Map<CapabilityInstanceId, CapabilityId> capabilityMap;
	Map<CapabilityInstanceId, String> wiringMap;
	public final static boolean DEBUG = true;
	// Map<CapabilityInstanceId, Protocol> protocolMap;
	LoadingClientProtocol clientProtocol;
	LoadingServerProtocol serverProtocol;
	int loadingMechanism;

	// opcua
	private Communication opcua_comm;
	private Object opcua_server;
	private Object opcua_object;
	private int unique_id;

	private int getUnique_id() {
		return unique_id += 1;
	}

	public HandshakeFunctionalUnit(String host, int port) {
		clientProtocol = null;
		serverProtocol = null;
		capabilityMap = new HashMap<>();
		wiringMap = new HashMap<CapabilityInstanceId, String>();
		unique_id = 9;
		// OPCUA EXPOSE
		opcua_comm = new Communication();
		opcua_server = opcua_comm.getServerCommunication().createServer(host, port);
		Object rootObjectId = opcua_comm.getServerCommunication().createNodeNumeric(1, getUnique_id());
		opcua_object = opcua_comm.getServerCommunication().addObject(opcua_server, rootObjectId, "Handshak FU");

		opcua_comm.getServerCommunication().addMethod_string(opcua_comm.getServerCommunication(), opcua_server,
				opcua_object, getUnique_id(), "initiate_Loading", "input", "output");
		opcua_comm.getServerCommunication().addMethod_string(opcua_comm.getServerCommunication(), opcua_server,
				opcua_object, getUnique_id(), "initiate_Unloading", "input", "output");

		opcua_comm.getServerCommunication().addMethod_string(opcua_comm.getServerCommunication(), opcua_server,
				opcua_object, getUnique_id(), "setWiring", "input", "output");

		opcua_comm.getServerCommunication().addMethod_string(opcua_comm.getServerCommunication(), opcua_server,
				opcua_object, getUnique_id(), "complete", "input", "output");
		opcua_comm.getServerCommunication().addMethod_string(opcua_comm.getServerCommunication(), opcua_server,
				opcua_object, getUnique_id(), "stop", "input", "output");
		opcua_comm.getServerCommunication().addMethod_string(opcua_comm.getServerCommunication(), opcua_server,
				opcua_object, getUnique_id(), "reset", "input", "output");

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

	void initiateLoading(CapabilityInstanceId instanceId, String orderId) {
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

	public void setRequiredCapability(CapabilityInstanceId instanceId, CapabilityId typeId) {
		this.capabilityMap.put(instanceId, typeId);
		// this.protocolMap.put(instanceId, initCapability(instanceId));
		initCapability(instanceId);
	}

	public void setWiring(CapabilityInstanceId localCapabilityId, String remoteCapabiltyEntryPoint) { // Serveraddress+NodeID
		this.wiringMap.put(localCapabilityId, remoteCapabiltyEntryPoint);

	}

	public void initCapability(CapabilityInstanceId instanceId) {
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
