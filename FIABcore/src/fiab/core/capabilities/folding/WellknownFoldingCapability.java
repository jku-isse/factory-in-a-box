package fiab.core.capabilities.folding;

import ProcessCore.AbstractCapability;
import ProcessCore.Parameter;
import ProcessCore.ProcessCoreFactory;
import fiab.core.capabilities.ComparableCapability;
import fiab.core.capabilities.OPCUABasicMachineBrowsenames;

public interface WellknownFoldingCapability extends OPCUABasicMachineBrowsenames {

    public static String FOLDING_CAPABILITY_BASE_URI = "http://factory-in-a-box.fiab/capabilities/fold/shape/";

    public static String OPCUA_FOLD_REQUEST = "FOLD";

    public static enum SupportedShapes { //default static java.awt.Color
        BOX, HAT, BOAT
    }


    static AbstractCapability getFoldingShapeCapability(WellknownFoldingCapability.SupportedShapes shape) {
        ComparableCapability ac = new ComparableCapability();
        ac.setDisplayName("Fold "+ shape);
        ac.setID("Capability.Folding.Shape."+ shape);
        ac.setUri(generateFoldingCapabilityURI(shape));
        ac.getInputs().add(getShapeInputParameter());
        return ac;
    }

    public static String generateFoldingCapabilityURI(WellknownFoldingCapability.SupportedShapes shape) {
        return FOLDING_CAPABILITY_BASE_URI+shape;
    }

    public static String FOLDING_CAPABILITY_INPUT_SHAPE_VAR_NAME = "RD_1";

    static Parameter getShapeInputParameter() {
        Parameter inImage = ProcessCoreFactory.eINSTANCE.createParameter();
        inImage.setName(FOLDING_CAPABILITY_INPUT_SHAPE_VAR_NAME);
        inImage.setType("String");
        return inImage;
    }

}
