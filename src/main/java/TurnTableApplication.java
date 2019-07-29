import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import turnTable.TurnTable;

public class TurnTableApplication {

    public static void main(String[] args) {
        new TurnTable(MotorPort.A, MotorPort.B, SensorPort.S1, 90);
    }
}
