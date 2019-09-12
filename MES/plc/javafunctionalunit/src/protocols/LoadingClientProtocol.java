/**
   [Class description.  The first sentence should be a meaningful summary of the class since it
   will be displayed as the class summary on the Javadoc package page.]

   [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
   about desired improvements, etc.]
   @author Michael Bishara
   @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
   @author <A HREF="https://github.com/michaelanis14">[Github]</A>
   @date 5 Sep 2019
**/

package protocols;

import static helper.ClientLoadingStates.COMPLETED;
import static helper.ClientLoadingStates.COMPLETING;
import static helper.ClientLoadingStates.EXECUTE;
import static helper.ClientLoadingStates.IDLE;
import static helper.ClientLoadingStates.INITIATED;
import static helper.ClientLoadingStates.INITIATING;
import static helper.ClientLoadingStates.READY;
import static helper.ClientLoadingStates.STARTING;
import static helper.ClientLoadingStates.STOPPED;
import static helper.ClientLoadingStates.STOPPING;

import communication.Communication;
import helper.ClientLoadingStates;

public class LoadingClientProtocol extends Protocol {
	int currentState;
	String serverPath;
	String orderId;
	Object opcua_client;

	public LoadingClientProtocol() {
		// super();
		comm = new Communication();
		opcua_client = comm.getClientCommunication().initClient();

		// .getClientCommunication().initClient();
		// Object opcua_client = ;

	}

	public final int getCurrentState() {
		return currentState;
	}

	public void idle() {
		// comm.getClientCommunication().ClientSubtoNode(jClientAPIBase, client, nodeID)
	}

	public void starting() {

		new Thread(new Runnable() {
			@Override
			public void run() {
				comm.getClientCommunication().clientConnect(comm.getClientCommunication(), opcua_client, serverPath);

			}
		}).start();

	}

	private void initiating() {
		// TODO Auto-generated method stub

	}

	/**
	 *
	 */
	private void initiated() {
		// TODO Auto-generated method stub

	}

	private void ready() {
		// TODO Auto-generated method stub

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

	public void fireTrigger(ClientLoadingStates states) {

		switch (states) {
		case IDLE:
			currentState = IDLE.ordinal();
			idle();
			break;
		case STARTING:
			currentState = STARTING.ordinal();
			starting();
			break;
		case INITIATING:
			currentState = INITIATING.ordinal();
			initiating();
			break;
		case INITIATED:
			currentState = INITIATED.ordinal();
			initiated();
			break;
		case READY:
			currentState = READY.ordinal();
			ready();
			break;

		case EXECUTE:
			currentState = EXECUTE.ordinal();
			execute();
			break;
		case COMPLETING:
			currentState = COMPLETING.ordinal();
			completing();
			break;
		case COMPLETED:
			currentState = COMPLETED.ordinal();
			compelete();
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

	public void setServerPath(String serverPath) {
		this.serverPath = serverPath;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
}
