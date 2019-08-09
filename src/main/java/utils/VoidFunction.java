package utils;

import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

import java.util.function.Consumer;

/**
 * Represents a void function for opc_ua.
 * The Method needs a String input in order to work properly and currently only supports one argument.
 * TODO test and later add other input/output types when they are available
 */
public class VoidFunction extends ServerAPIBase {

    private final Consumer<String> consumer;

    public VoidFunction(Consumer<String> function){
        this.consumer = function;
    }

    @Override
    public void methods_callback(ServerAPIBase jAPIBase, UA_NodeId methodId, UA_NodeId objectId, String input, String output) {
        super.methods_callback(jAPIBase, methodId, objectId, input, output);
        consumer.accept(input);
        jAPIBase.setMethodOutput("Success");
    }
}