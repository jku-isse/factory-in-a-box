package fiab.core.capabilities;

import ProcessCore.impl.AbstractCapabilityImpl;

public class ComparableCapability extends AbstractCapabilityImpl {



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		AbstractCapabilityImpl other = (AbstractCapabilityImpl) obj;
		if (id == null) {
			if (other.getID() != null)
				return false;
		} else if (!id.equals(other.getID()))
			return false;
		if (uri == null) {
			if (other.getUri() != null)
				return false;
		} else if (!uri.equals(other.getUri()))
			return false;
		return true;
	}
}
