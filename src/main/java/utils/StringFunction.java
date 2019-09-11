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

    /**
     * Creates a String to String function to use in the method callback.
     * @param function string to string function. If the String should not change use x -> x
     */
    public StringFunction(Function<String, String> function) {
        this.function = function;
    }

    /**
     * Returns the function
     * @return function
     */
    public Function<String, String> getFunction() {
        return function;
    }

    /**
     * Callback that executes the function and sets it's return value as the output.
     * @param methodId method id
     * @param objectId object id
     * @param input input
     * @param output output
     * @param jAPIBase serverAPIBase of the server
     */
    @Override
    public void methods_callback(UA_NodeId methodId, UA_NodeId objectId, String input, String output, ServerAPIBase jAPIBase) { ;
        setMethodOutput(methodId, function.apply(input));
    }
}
