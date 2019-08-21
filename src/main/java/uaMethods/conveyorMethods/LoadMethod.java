package uaMethods.conveyorMethods;

import functionalUnitBase.ConveyorBase;
import open62Wrap.*;
import utils.StringFunction;

import java.util.HashMap;
import java.util.function.Function;

public class LoadMethod {

    private ConveyorBase conveyor;
    private UA_NodeId nodeId;

    public LoadMethod(ConveyorBase conveyor) {
        this.conveyor = conveyor;
    }

    public void addMethod(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId conveyorFolder) {
        UA_LocalizedText localeIn = new UA_LocalizedText();
        localeIn.setLocale("en-US");
        localeIn.setText("Load Method");

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
        methodLocale.setText("Load Conveyor");

        UA_MethodAttributes methodAttributes = new UA_MethodAttributes();
        methodAttributes.setDescription(methodLocale);
        methodAttributes.setDisplayName(methodLocale);
        methodAttributes.setExecutable(true);
        methodAttributes.setUserExecutable(true);
        serverAPIBase.addMethod(server, conveyorFolder, open62541.UA_NODEID_NUMERIC(1, 21),
                input, output, methodAttributes, new StringFunction(x -> {
                    conveyor.load();
                    return "Loading Successful";
                }));
    }
}
