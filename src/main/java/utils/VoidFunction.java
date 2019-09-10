package utils;

import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

import java.util.function.Consumer;

/**
 * Represents a void function for opc_ua.
 * The Method needs a String input in order to work properly and currently only supports one argument.
 */
public class VoidFunction extends ServerAPIBase {

    private final Consumer<String> consumer;

    public VoidFunction(Consumer<String> function){
        this.consumer = function;
    }

    @Override
    public void methods_callback(UA_NodeId methodId, UA_NodeId objectId, String input, String output, ServerAPIBase jAPIBase) {
        consumer.accept(input);
        jAPIBase.setMethodOutput(methodId, "Success");
    }
}