package functional_unit_dummys;

import functional_unit_base.ConveyorBase;
import open62Wrap.*;
import utils.StringFunction;

public class ConveyorDummy extends ConveyorBase {

    @Override
    public void load() {
        System.out.println("Load was called");
    }

    @Override
    public void unload() {
        System.out.println("Unload was called");
    }

    @Override
    public void pause() {
        System.out.println("Pause was called");
    }

    @Override
    public void reset() {
        System.out.println("Resetting conveyor");
    }

    @Override
    public void stop() {
        System.out.println("Stopping conveyor");
    }

    @Override
    public void addServerConfig(SWIGTYPE_p_UA_Server server ,ServerAPIBase serverAPIBase) {
        UA_LocalizedText localeIn = new UA_LocalizedText();
        localeIn.setLocale("en-US");
        localeIn.setText("Type in your name");

        UA_LocalizedText localeOut = new UA_LocalizedText();
        localeOut.setLocale("en-US");
        localeOut.setText("Success?");
        UA_Argument input = new UA_Argument();

        input.setDescription(localeIn);
        input.setName("Input");
        input.setDataType(serverAPIBase.getDataTypeNode(open62541.UA_TYPES_STRING));
        input.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_Argument output = new UA_Argument();
        output.setDescription(localeOut);
        output.setDataType(serverAPIBase.getDataTypeNode(open62541.UA_TYPES_STRING));
        output.setValueRank(open62541.UA_VALUERANK_SCALAR);

        UA_LocalizedText methodLocale = new UA_LocalizedText();
        methodLocale.setText("SayHello");

        UA_MethodAttributes methodAttributes = new UA_MethodAttributes();
        methodAttributes.setDescription(methodLocale);
        methodAttributes.setDisplayName(methodLocale);
        methodAttributes.setExecutable(true);
        methodAttributes.setUserExecutable(true);
        serverAPIBase.addMethod(server, input, output, methodAttributes, new StringFunction(x -> {
            load();
            return "Success";
        }));
    }


}
