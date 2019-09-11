package utils;

import open62Wrap.ServerAPIBase;
import open62Wrap.UA_NodeId;

import java.util.function.Consumer;

/**
 * Represents a Monitored Item for opc_ua. We can give it a method with return type void.
 * When the monitored item changes, the method gets fired.
 */
public class MonitoredItem extends ServerAPIBase {

    private final Consumer<Integer> consumer;

    /**
     * Creates a Monitored item object.
     * @param consumer void method to execute. If none is necessary use x -> x
     */
    public MonitoredItem(Consumer<Integer> consumer){
        //TODO remove consumer as the result is currently ignored
        this.consumer = consumer;
    }

    /**
     * Fires when the monitored item has changed
     * @param nodeId node of the item
     * @param value value of the input
     */
    @Override
    public void monitored_itemChanged(UA_NodeId nodeId, int value) {
        super.monitored_itemChanged(nodeId, value);
        consumer.accept(value);
    }
}
