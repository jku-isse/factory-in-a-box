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

import helper.Capability;
import helper.CapabilityInstanceId;

public class FunctionalUnit {

	Capability capability;

	CapabilityInstanceId localCapabilityId;
	String remoteCapabilityId;
	CapabilityInstanceId localRole;

	FunctionalUnit() {
		this.capability = new Capability();
	}

}
