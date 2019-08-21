package uaMethods.turningMethods;

import functionalUnitBase.TurningBase;
import open62Wrap.*;
import turnTable.TurnTableOrientation;
import utils.StringFunction;

public class TurnToMethod {

    private TurningBase turning;

    public TurnToMethod(TurningBase turning) {
        this.turning = turning;
    }

    public void addMethod(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId turningFolder) {
        UA_LocalizedText localeIn = new UA_LocalizedText();
        localeIn.setLocale("en-US");
        localeIn.setText("TurnTo Method");

        UA_LocalizedText localeOut = new UA_LocalizedText();
        localeOut.setLocale("en-US");
        localeOut.setText("Success?");
        UA_Argument input = new UA_Argument();

        input.setDescription(localeIn);
        input.setName("Input");     //TODO see data type of input and change accordingly
        input.setDataType(serverAPIBase.getDataTypeNode(open62541.UA_TYPES_STRING));
        input.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_Argument output = new UA_Argument();
        output.setDescription(localeOut);
        output.setDataType(serverAPIBase.getDataTypeNode(open62541.UA_TYPES_STRING));
        output.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_LocalizedText methodLocale = new UA_LocalizedText();
        methodLocale.setText("TurnTo");

        UA_MethodAttributes methodAttributes = new UA_MethodAttributes();
        methodAttributes.setDescription(methodLocale);
        methodAttributes.setDisplayName(methodLocale);
        methodAttributes.setExecutable(true);
        methodAttributes.setUserExecutable(true);
        serverAPIBase.addMethod(server, turningFolder, open62541.UA_NODEID_NUMERIC(1, 33),
                input, output, methodAttributes, new StringFunction(x -> {
                    this.turning.turnTo(TurnTableOrientation.createFromInt(Integer.parseInt(x)));
                    return "Turned to " + x;
                }));
    }
}
