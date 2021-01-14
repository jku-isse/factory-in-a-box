package fiab.mes.transport.msg;

import main.java.fiab.core.capabilities.events.TimedEvent;

public class RegisterTransportRequestStatusResponse extends TimedEvent{

	protected RegisterTransportRequest originalRequest;
	protected ResponseType response;
	protected String message;	
	
	public RegisterTransportRequestStatusResponse(RegisterTransportRequest originalRequest, ResponseType response,
			String message) {
		super();
		this.originalRequest = originalRequest;
		this.response = response;
		this.message = message;
	}

	public RegisterTransportRequest getOriginalRequest() {
		return originalRequest;
	}

	public ResponseType getResponse() {
		return response;
	}

	public String getMessage() {
		return message;
	}

	
	
	@Override
	public String toString() {
		return "RTRStatusResponse [originalRequest=" + originalRequest.getOrderId() + ", response=" + response
				+ ", message=" + message + "]";
	}



	public static enum ResponseType {
		COMPLETED, //successfully executed 		
		QUEUED, // possible but transport modules not ready yet
		CANCELED, // successfully removed before being transported
		ISSUED, // fowarded requests to individual modules,
		ABORTED, // stopped somewhere in transite, req manual intervention
		FAILED_IN_TRANSPORT, // transport module signal failure
		MISSING_TRANSPORT_MODULE, UNSUPPORTED_TRANSIT_POSITION, UNSUPPORTED_ENDPOINT_POSITIONS, NO_ROUTE // not possible to transport, request not possible to execute at the moment  
	}
}
