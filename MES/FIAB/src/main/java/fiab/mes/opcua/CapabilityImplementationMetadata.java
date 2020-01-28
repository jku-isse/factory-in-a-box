package fiab.mes.opcua;

public class CapabilityImplementationMetadata {
	String implId;
	String capabilityURI;
	ProvOrReq provOrReq;
		
	public static enum ProvOrReq { REQUIRED, PROVIDED }

	public CapabilityImplementationMetadata(String implId, String capabilityURI, ProvOrReq provOrReq) throws MetadataInsufficientException {
		super();
		if (implId == null || capabilityURI == null || provOrReq == null) 
			throw new MetadataInsufficientException("Null Values not allowed for initializing CapabilityImplementationMetadata");
		this.implId = implId;
		this.capabilityURI = capabilityURI;
		this.provOrReq = provOrReq;
	}

	public String getImplId() {
		return implId;
	}

	public String getCapabilityURI() {
		return capabilityURI;
	}

	public ProvOrReq getProvOrReq() {
		return provOrReq;
	}
	
	
	public class MetadataInsufficientException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3088924409213503398L;
		
		public MetadataInsufficientException(String msg) {
			super(msg);
		}
		
	}
}
