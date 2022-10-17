package fiab.core.capabilities.roboticArm;

import ProcessCore.AbstractCapability;
import ProcessCore.Parameter;
import ProcessCore.ProcessCoreFactory;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;
import fiab.core.capabilities.functionalunit.BasicFUBehaviour;
import fiab.core.capabilities.plotting.WellknownPlotterCapability;

public interface RoboticArmCapability extends BasicFUBehaviour, OPCUABasicMachineBrowsenames {


    String ROBOTIC_ARM_CAPABILITY_BASE_URI = "http://factory-in-a-box.fiab/capabilities/roboticarm";

    String OPCUA_PICK_REQUEST = "PickPart";

    enum SupportedParts {
        WASHER, SCREW, NUT
    }

    static AbstractCapability getPickPartCapability() {
        ComparableCapability ac = new ComparableCapability();
        ac.setDisplayName("PickPart ");
        ac.setID("Capability.Plotting.Color.");
        ac.setUri(ROBOTIC_ARM_CAPABILITY_BASE_URI);
        ac.getInputs().add(getImageInputParameter());
        return ac;
    }

    String ROBOTIC_ARM_CAPABILITY_INPUT_PART_VAR_NAME = "RD_1";

    static Parameter getImageInputParameter() {
        Parameter inImage = ProcessCoreFactory.eINSTANCE.createParameter();
        inImage.setName(ROBOTIC_ARM_CAPABILITY_INPUT_PART_VAR_NAME);
        inImage.setType("String");
        return inImage;
    }
}
