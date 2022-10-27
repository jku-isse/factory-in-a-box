import hardware.ConveyorHardware;
import hardware.mock.ConveyorMockHardware;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConveyorHAL {

    @Test
    public void testMockConveyorHAL(){
        ConveyorHardware hardware = new ConveyorMockHardware();
        assertFalse(hardware.isLoadingSensorDetectingPallet());
        assertFalse(hardware.isUnloadingSensorDetectingPallet());
        //Load pallet
        hardware.startMotorForLoading();
        while(!hardware.isLoadingSensorDetectingPallet()){
            //wait
        }
        //Pallet loaded
        hardware.stopConveyorMotor();
        assertTrue(hardware.isLoadingSensorDetectingPallet());
        assertTrue(hardware.isUnloadingSensorDetectingPallet());
        //Unload pallet
        hardware.startMotorForUnloading();
        while(hardware.isUnloadingSensorDetectingPallet()){
            //wait
        }
        //Pallet unloaded
        hardware.stopConveyorMotor();
        assertFalse(hardware.isLoadingSensorDetectingPallet());
        assertFalse(hardware.isUnloadingSensorDetectingPallet());
    }
}
