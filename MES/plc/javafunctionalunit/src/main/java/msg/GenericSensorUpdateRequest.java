package msg;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GenericSensorUpdateRequest {

    public enum SensorId {
        SENSOR_LOADING, SENSOR_UNLOADING, SENSOR_HOMING
    }
    public static abstract class SensorUpdateRequest {
        protected String machineId;

        public String getMachineId() {
            return machineId;
        }

        public SensorUpdateRequest(String machineId) {
            super();
            this.machineId = machineId;
        }

    }

    public static class Stop extends SensorUpdateRequest {

        public Stop(String machineId) {
            super(machineId);
        }

        @Override
        public String toString() {
            return "Stop [" + machineId + "]";
        }
    }

    public static class Reset extends SensorUpdateRequest {

        public Reset(String machineId) {
            super(machineId);
        }

        @Override
        public String toString() {
            return "Reset [" + machineId + "]";
        }

    }

}
