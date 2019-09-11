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
package helper;

public class Capability {
	CapabilityInstanceId instanceId;
	CapabilityId typeId;

	public CapabilityInstanceId getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(CapabilityInstanceId instanceId) {
		this.instanceId = instanceId;
	}

	public CapabilityId getTypeId() {
		return typeId;
	}

	public void setTypeId(CapabilityId typeId) {
		this.typeId = typeId;
	}

	public Capability() {
		this.instanceId = null;
		this.typeId = null;
	}

}
