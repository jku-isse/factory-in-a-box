import functional_unit_dummys.ConveyorDummy;
import functional_unit_dummys.LoadingDummy;
import functional_unit_dummys.ProcessDummy;
import robot_base.RobotBase;

public class RobotTestApplication {

    public static void main(String[] args) {
        new RobotBase(new LoadingDummy(), new ConveyorDummy()).runServer();
    }
}
