package msg;

public class GenericMachineRequests {

	public static abstract class BaseRequest {
		protected String machineId;

		public String getMachineId() {
			return machineId;
		}

		public BaseRequest(String machineId) {
			super();
			this.machineId = machineId;
		}
		
	}
	
	public static class Stop extends BaseRequest {

		public Stop(String machineId) {
			super(machineId);
		}
		
		@Override
		public String toString() {
			return "Stop [" + machineId + "]";
		}
	}
	
	public static class Reset extends BaseRequest {

		public Reset(String machineId) {
			super(machineId);
		}

		@Override
		public String toString() {
			return "Reset [" + machineId + "]";
		}
		
	}
	
}
