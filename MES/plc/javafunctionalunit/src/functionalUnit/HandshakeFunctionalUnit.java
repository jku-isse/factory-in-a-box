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

import helper.CapabilityId;
import helper.CapabilityInstanceId;
import helper.HandshakeStates;
import protocols.LoadingClientProtocol;

public class HandshakeFunctionalUnit extends FunctionalUnit {

	public HandshakeFunctionalUnit() {

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

	}

	public void starting() {

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

	public void initiateUnloading(String direction, String OrderId) {
		EngageInUnLoading = new LoadingClientProtocol();
	}

	void initiateLoading(String direction, String OrderId) {

	}

	public void setRequiredCapability(CapabilityInstanceId instanceId, CapabilityId typeId) {
		this.capability.setInstanceId(instanceId);
		this.capability.setTypeId(typeId);

	}

	public void setWiring(CapabilityInstanceId localCapabilityId, String remoteCapabilityId,
			CapabilityInstanceId localRole) {
		this.localCapabilityId = localCapabilityId;
		this.remoteCapabilityId = remoteCapabilityId;
		this.localRole = localRole;
	}

}
