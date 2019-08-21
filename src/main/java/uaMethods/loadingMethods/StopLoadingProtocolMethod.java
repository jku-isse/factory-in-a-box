package uaMethods.loadingMethods;

import functionalUnitBase.LoadingProtocolBase;
import open62Wrap.*;
import utils.StringFunction;

public class StopLoadingProtocolMethod {

    private LoadingProtocolBase loadingProtocol;

    public StopLoadingProtocolMethod(LoadingProtocolBase loadingProtocol) {
        this.loadingProtocol = loadingProtocol;
    }

    public void addMethod(SWIGTYPE_p_UA_Server server, ServerAPIBase serverAPIBase, UA_NodeId loadingFolder) {
        UA_LocalizedText localeIn = new UA_LocalizedText();
        localeIn.setLocale("en-US");
        localeIn.setText("Stop Method Loading Protocol");

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
        methodLocale.setText("Stop Loading Protocol");

        UA_MethodAttributes methodAttributes = new UA_MethodAttributes();
        methodAttributes.setDescription(methodLocale);
        methodAttributes.setDisplayName(methodLocale);
        methodAttributes.setExecutable(true);
        methodAttributes.setUserExecutable(true);
        serverAPIBase.addMethod(server, loadingFolder, open62541.UA_NODEID_NUMERIC(1, 15),
                input, output, methodAttributes, new StringFunction(x -> {
                    this.loadingProtocol.stop();
                    return "Stop Successful";
                }));
    }
}
