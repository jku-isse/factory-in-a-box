package uaMethods.turningMethods;

import functionalUnitBase.TurningBase;
import open62Wrap.*;
import utils.StringFunction;

public class ResetTurningMethod {

    private TurningBase turning;

    public ResetTurningMethod(TurningBase turning) {
        this.turning = turning;
    }

    public void addMethod(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId turningFolder) {
        UA_LocalizedText localeIn = new UA_LocalizedText();
        localeIn.setLocale("en-US");
        localeIn.setText("Reset Method Turning");

        UA_LocalizedText localeOut = new UA_LocalizedText();
        localeOut.setLocale("en-US");
        localeOut.setText("Success?");
        UA_Argument input = new UA_Argument();

        input.setDescription(localeIn);
        input.setName("Input");     //TODO delete input as it is not necessary
        input.setDataType(serverAPIBase.getDataTypeNode(open62541.UA_TYPES_STRING));
        input.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_Argument output = new UA_Argument();
        output.setDescription(localeOut);
        output.setDataType(serverAPIBase.getDataTypeNode(open62541.UA_TYPES_STRING));
        output.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_LocalizedText methodLocale = new UA_LocalizedText();
        methodLocale.setText("Reset Turning");

        UA_MethodAttributes methodAttributes = new UA_MethodAttributes();
        methodAttributes.setDescription(methodLocale);
        methodAttributes.setDisplayName(methodLocale);
        methodAttributes.setExecutable(true);
        methodAttributes.setUserExecutable(true);
        serverAPIBase.addMethod(server, turningFolder, open62541.UA_NODEID_NUMERIC(1, 31),
                input, output, methodAttributes, new StringFunction(x -> {
                    turning.reset();
                    return "Resetting Successful";
                }));
    }
}
