import functionalUnitDummys.ConveyorDummy;
import functionalUnitDummys.LoadingDummy;
import robotBase.RobotBase;

public class RobotTestApplication {

    public static void main(String[] args) {
        new RobotBase(new LoadingDummy(), new ConveyorDummy()).runServer();
    }
}
