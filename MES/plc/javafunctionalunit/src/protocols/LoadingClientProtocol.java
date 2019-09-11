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

import static helper.LoadingStates.COMPLETED;
import static helper.LoadingStates.COMPLETING;
import static helper.LoadingStates.EXECUTE;
import static helper.LoadingStates.IDLE;
import static helper.LoadingStates.INITIATED;
import static helper.LoadingStates.INITIATING;
import static helper.LoadingStates.READY;
import static helper.LoadingStates.STARTING;
import static helper.LoadingStates.STOPPED;
import static helper.LoadingStates.STOPPING;

import communication.Communication;
import helper.LoadingStates;

public class LoadingClientProtocol extends Protocol {
	static int currentState;;

	public LoadingClientProtocol() {
		// super();
		comm = new Communication();
		Object opcua_client = comm.getClientCommunication().initClient();

		// .getClientCommunication().initClient();
		// Object opcua_client = ;
		new Thread(new Runnable() {
			@Override
			public void run() {
				comm.getClientCommunication().clientConnect(comm.getClientCommunication(), opcua_client,
						"opc.tcp://localhost:4840");

			}
		}).start();

		Object statusNodeID = comm.getClientCommunication().getNodeByName(opcua_client, "Status"); // get

		System.out.println(statusNodeID); // server by name
		// int subId =
		// comm.getClientCommunication().clientSubtoNode(comm.getClientCommunication(),
		// opcua_client, statusNodeID); // subscribe
		// to
		// changes at the

	}

	public final int getCurrentState() {
		return currentState;
	}

	public void idle() {
		// comm.getClientCommunication().ClientSubtoNode(jClientAPIBase, client, nodeID)
	}

	public void starting() {

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

	public void fireTrigger(LoadingStates states) {

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

}
