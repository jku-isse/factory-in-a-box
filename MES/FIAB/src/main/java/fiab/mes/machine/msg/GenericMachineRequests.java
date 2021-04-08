package fiab.mes.machine.msg;

import fiab.tracing.actor.messages.TracingHeader;

public class GenericMachineRequests {

	public static abstract class BaseRequest implements TracingHeader {
		protected String machineId;
		private String header;

		public String getMachineId() {
			return machineId;
		}

		public BaseRequest(String machineId) {
			this(machineId, "");
		}

		public BaseRequest(String machineId, String header) {
			super();
			this.machineId = machineId;
			this.header = header;
		}

		@Override
		public String getTracingHeader() {
			return this.header;
		}

		@Override
		public void setTracingHeader(String header) {
			this.header = header;
		}

	}

	public static class Stop extends BaseRequest {

//		public Stop(String machineId,String header) {
//			super(machineId,header);
//		}
		public Stop(String machineId ) {
			super(machineId);
		}

		@Override
		public String toString() {
			return "Stop [" + machineId + "]";
		}
	}

	public static class Reset extends BaseRequest {

		public Reset(String machineId,String header) {
			super(machineId,header);
		}
		
		public Reset(String machineId ) {
			super(machineId);
		}

		@Override
		public String toString() {
			return "Reset [" + machineId + "]";
		}

	}

}
