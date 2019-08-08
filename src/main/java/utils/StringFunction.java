package utils;

import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

import java.util.function.Function;

/**
 * Represents a string function for opc_ua.
 * The Method needs a String input and output in order to work properly and currently only supports one argument.
 * TODO test and later add other input/output types when they are available
 */
public class StringFunction extends ServerAPIBase {

    private final Function<String, String> function;

    public StringFunction(Function<String, String> function){
        this.function = function;
    }

    @Override
    public void methods_callback(ServerAPIBase jAPIBase, UA_NodeId methodId, UA_NodeId objectId, String input, String output) {
        super.methods_callback(jAPIBase, methodId, objectId, input, output);
        jAPIBase.setMethodOutput(function.apply(input));
    }
}
