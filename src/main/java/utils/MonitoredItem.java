package utils;

import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a Monitored Item for opc_ua. We can give it a method with return type void.
 * When the monitored item changes, the method gets fired.
 * TODO test and later add other input types when they are available
 *
 */
public class MonitoredItem extends ServerAPIBase {

    private final Consumer<Integer> consumer;

    public MonitoredItem(Consumer<Integer> consumer){
        this.consumer = consumer;
    }

    @Override
    public void monitored_itemChanged(UA_NodeId nodeId, int value) {
        super.monitored_itemChanged(nodeId, value);
        consumer.accept(value);
    }
}
