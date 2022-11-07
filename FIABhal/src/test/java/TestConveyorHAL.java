import hardware.ConveyorHardware;
import hardware.mock.ConveyorMockHardware;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;


import static org.awaitility.Awaitility.await;
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
        await().until(() -> hardware.isLoadingSensorDetectingPallet());
        //while(!hardware.isLoadingSensorDetectingPallet()){
        //    assertTrue(hardware.isUnloadingSensorDetectingPallet());
        //    assertFalse(hardware.isLoadingSensorDetectingPallet());
        //}
        //Pallet loaded
        hardware.stopConveyorMotor();
        assertTrue(hardware.isLoadingSensorDetectingPallet());
        assertTrue(hardware.isUnloadingSensorDetectingPallet());
        //Unload pallet
        hardware.startMotorForUnloading();
        //while(hardware.isUnloadingSensorDetectingPallet()){
        //    //wait
        //}
        await().until(() -> !hardware.isUnloadingSensorDetectingPallet());
        //Pallet unloaded
        hardware.stopConveyorMotor();
        assertFalse(hardware.isLoadingSensorDetectingPallet());
        assertFalse(hardware.isUnloadingSensorDetectingPallet());
    }
}
