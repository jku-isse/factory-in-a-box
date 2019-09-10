package utils;

import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

import java.util.function.Function;

/**
 * Represents a string function for opc_ua.
 * The Method needs a String input and output in order to work properly and currently only supports one argument.
 */
public class StringFunction extends ServerAPIBase {

    private final Function<String, String> function;

    public StringFunction(Function<String, String> function) {
        this.function = function;
    }

    public Function<String, String> getFunction() {
        return function;
    }

    @Override
    public void methods_callback(UA_NodeId methodId, UA_NodeId objectId, String input, String output, ServerAPIBase jAPIBase) {
        //super.methods_callback(methodId, objectId, input, output, jAPIBase);
        setMethodOutput(methodId, function.apply(input));
    }
}
