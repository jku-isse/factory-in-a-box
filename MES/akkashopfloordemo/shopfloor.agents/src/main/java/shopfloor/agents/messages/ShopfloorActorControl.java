package shopfloor.agents.messages;

public class ShopfloorActorControl {

	public class ControlRequest {
		protected DefaultControlRequests action;

		@Deprecated
		public ControlRequest() {}

		public ControlRequest(DefaultControlRequests action) {
			super();
			this.action = action;
		}

		public DefaultControlRequests getAction() {
			return action;
		}

		public void setAction(DefaultControlRequests action) {
			this.action = action;
		}
	}
	
	public class ControlResponse {
		protected String shopfloorActorId;
		protected DefaultControlRequests responseToAction;
		protected int responseCode; // reusing HTTP response/error codes
		
		@Deprecated
		public ControlResponse() {}
		
		public ControlResponse(String shopfloorActorId, DefaultControlRequests responseToAction, int responseCode) {
			super();
			this.shopfloorActorId = shopfloorActorId;
			this.responseToAction = responseToAction;
			this.responseCode = responseCode;
		}
		
		public String getShopfloorActorId() {
			return shopfloorActorId;
		}
		public void setShopfloorActorId(String shopfloorActorId) {
			this.shopfloorActorId = shopfloorActorId;
		}
		public DefaultControlRequests getResponseToAction() {
			return responseToAction;
		}
		public void setResponseToAction(DefaultControlRequests responseToAction) {
			this.responseToAction = responseToAction;
		}
		public int getResponseCode() {
			return responseCode;
		}
		public void setResponseCode(int responseCode) {
			this.responseCode = responseCode;
		}				
	}
	
	public enum DefaultControlRequests{ 
		START, STOP, RESET, PAUSE, UNPAUSE
	}
	
}
