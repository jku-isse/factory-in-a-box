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
package controller;

import functionalUnit.HandshakeFunctionalUnit;
import helper.CapabilityId;
import helper.CapabilityInstanceId;

public class Controller {

	/**
	 * main method for testing purposes. calling the constructor then running the
	 * server in a separate thread.
	 */
	public static void main(String[] args) {

		HandshakeFunctionalUnit hsFU = new HandshakeFunctionalUnit("localhost", 4840);

		// Process Engine
		hsFU.setRequiredCapability(CapabilityInstanceId.NORTH_CLIENT, CapabilityId.EngageInUnLoading);
		hsFU.setWiring(CapabilityInstanceId.NORTH_CLIENT, "opc.tcp://localhost:4840");
		hsFU.initiateUnloading("NORTH", "0001");
		System.err.println("Controller Main Started");

	}
}