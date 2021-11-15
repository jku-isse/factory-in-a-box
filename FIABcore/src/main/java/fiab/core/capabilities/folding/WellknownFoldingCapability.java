package fiab.core.capabilities.folding;

import ProcessCore.AbstractCapability;
import ProcessCore.Parameter;
import ProcessCore.ProcessCoreFactory;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;

public interface WellknownFoldingCapability extends OPCUABasicMachineBrowsenames {

    public static String FOLDING_CAPABILITY_BASE_URI = "http://factory-in-a-box.fiab/capabilities/fold/box";

    public static String OPCUA_FOLD_REQUEST = "Fold";

    static AbstractCapability getFoldingShapeCapability() {
        ComparableCapability ac = new ComparableCapability();
        ac.setDisplayName("Fold");
        ac.setID("Capability.Folding.BOX");
        ac.setUri(generateFoldingCapabilityURI());
        ac.getInputs().add(getShapeInputParameter());
        return ac;
    }

    public static String generateFoldingCapabilityURI() {
        return FOLDING_CAPABILITY_BASE_URI;
    }

    public static String FOLDING_CAPABILITY_INPUT_SHAPE_VAR_NAME = "RD_1";

    static Parameter getShapeInputParameter() {
        Parameter inImage = ProcessCoreFactory.eINSTANCE.createParameter();
        inImage.setName(FOLDING_CAPABILITY_INPUT_SHAPE_VAR_NAME);
        inImage.setType("String");
        return inImage;
    }

}
