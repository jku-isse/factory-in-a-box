/**
   [Class description.  The first sentence should be a meaningful summary of the class since it
   will be displayed as the class summary on the Javadoc package page.]

   [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
   about desired improvements, etc.]
   @author Michael Bishara
   @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
   @author <A HREF="https://github.com/michaelanis14">[Github]</A>
   @date 4 Sep 2019
**/
package protocols;

import communication.Communication;
import helper.CapabilityType;

import java.util.List;


public class Protocol {
	public Protocol() {
		comm = new Communication();
	}

	CapabilityType typeId;
	List<String> methods;
	List<String> monitoredItems;
	Communication comm;

	public Protocol(CapabilityType typeid, List<String> methods, List<String> monitoredItems) {
		this.methods = methods;
		this.monitoredItems = monitoredItems;
		this.typeId = typeid;
	}
}
