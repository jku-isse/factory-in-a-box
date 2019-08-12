import functionalUnitDummys.ConveyorDummy;
import functionalUnitDummys.LoadingDummy;
import functionalUnitDummys.ProcessDummy;
import functionalUnitDummys.TurningDummy;
import robotBase.RobotBase;

public class RobotTestApplication {

    public static void main(String[] args) {
        //TODO fix method callback
        new RobotBase(new LoadingDummy(), new ConveyorDummy(), new TurningDummy(), new ProcessDummy())
                .runServer();
    }
}
